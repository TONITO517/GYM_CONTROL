package modelo;

import excepciones.DatosInvalidosException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import modelo.estrategia.EstrategiaPrecio;
import modelo.persistencia.ActividadDAO;
import modelo.persistencia.CatalogoDAO;
import modelo.persistencia.ConexionSqlite;
import modelo.persistencia.MembresiaDAO;
import modelo.persistencia.SocioDAO;

import static modelo.Validador.*;

public final class GymModelo {
    private final List<GymListener> listeners = new ArrayList<>();
    private final ConexionSqlite base;
    private final SocioDAO socios = new SocioDAO();
    private final CatalogoDAO catalogo = new CatalogoDAO();
    private final MembresiaDAO membresias = new MembresiaDAO();
    private final ActividadDAO actividad = new ActividadDAO();
    private final Clock reloj;

    // El resultado y los datos para la pantalla salen de la misma transacción.
    private static final class Cambio<T> {
        private final T resultado;
        private final EstadoGym estado;

        private Cambio(T resultado, EstadoGym estado) {
            this.resultado = resultado;
            this.estado = estado;
        }
    }

    public GymModelo(Path archivo, Clock reloj) {
        base = new ConexionSqlite(archivo);
        this.reloj = reloj;
    }

    public void inicializar(boolean demostracion) {
        base.inicializar(demostracion);
    }

    // --- Observadores y carga de datos ---

    public synchronized void agregarListener(GymListener listener) {
        listeners.add(listener);
    }

    public synchronized void eliminarListener(GymListener listener) {
        listeners.remove(listener);
    }

    private void notificarCambios(EstadoGym estado) {
        List<GymListener> observadores;
        synchronized (this) {
            observadores = List.copyOf(listeners);
        }
        for (GymListener listener : observadores) {
            try {
                listener.gymActualizado(estado);
            } catch (RuntimeException error) {
                // Un error de un observador no invalida una operación ya confirmada.
                System.getLogger(GymModelo.class.getName()).log(System.Logger.Level.ERROR, "Error de observador", error);
            }
        }
    }

    private <T> T cambiar(ConexionSqlite.Operacion<T> operacion) {
        Cambio<T> cambio = base.transaccion(conexion -> {
            T resultado = operacion.ejecutar(conexion);
            return new Cambio<>(resultado, leerEstado(conexion));
        });
        // La base ya confirmó el cambio cuando se avisa a las vistas.
        notificarCambios(cambio.estado);
        return cambio.resultado;
    }

    private EstadoGym leerEstado(Connection conexion) throws SQLException {
        return new EstadoGym(LocalDateTime.now(reloj), socios.listar(conexion), catalogo.sedes(conexion),
                catalogo.planes(conexion), membresias.listar(conexion), membresias.pagos(conexion),
                actividad.asistencias(conexion), catalogo.entrenadores(conexion), actividad.clases(conexion),
                actividad.reservas(conexion));
    }

    public EstadoGym consultar() {
        return base.transaccion(this::leerEstado);
    }

    public void actualizarVistas() {
        notificarCambios(consultar());
    }

    // --- Socios y acciones con historial ---

    public Socio registrarSocio(String dni, String nombres, String apellidos, String telefono) {
        return insertarSocio(new Socio(0, dni, nombres, apellidos, telefono, LocalDate.now(reloj), Socio.Estado.ACTIVO));
    }

    public Socio insertarSocio(Socio socio) {
        Socio limpio = validarSocio(socio);
        return cambiar(conexion -> {
            exigir(socios.porDni(conexion, limpio.getDni()) == null, "El DNI ya pertenece a un socio.");
            int id = socios.insertar(conexion, limpio);
            return socio(conexion, id);
        });
    }

    private Socio validarSocio(Socio socio) {
        return new Socio(socio.getId(), dni(socio.getDni()), texto(socio.getNombres(), "nombres"),
                texto(socio.getApellidos(), "apellidos"), texto(socio.getTelefono(), "teléfono"),
                socio.getFechaRegistro(), socio.getEstado());
    }

    public Socio reemplazarSocio(Socio anterior, Socio nuevo) {
        Socio limpio = validarSocio(nuevo);
        exigir(anterior.getId() == limpio.getId(), "Los cambios deben corresponder al mismo socio.");
        return cambiar(conexion -> {
            exigir(socio(conexion, anterior.getId()).mismosDatos(anterior), "El socio cambió. Actualiza la lista y vuelve a intentar.");
            Socio titularDni = socios.porDni(conexion, limpio.getDni());
            exigir(titularDni == null || titularDni.getId() == limpio.getId(), "El DNI ya pertenece a otro socio.");
            socios.actualizar(conexion, limpio);
            return socio(conexion, limpio.getId());
        });
    }

    public void eliminarRegistroSocio(Socio registrado) {
        cambiar(conexion -> {
            exigir(socio(conexion, registrado.getId()).mismosDatos(registrado), "El socio cambió desde el registro.");
            exigir(!socios.tieneHistorial(conexion, registrado.getId()),
                    "El socio ya tiene membresías, asistencias o reservas asociadas. Puedes suspenderlo.");
            socios.eliminar(conexion, registrado.getId());
            return null;
        });
    }

    // --- Sedes y planes ---

    public int guardarSede(int id, String nombre, String direccion, int capacidad) {
        String nombreLimpio = texto(nombre, "nombre");
        String direccionLimpia = texto(direccion, "dirección");
        exigir(capacidad > 0, "La capacidad debe ser mayor que cero.");
        return cambiar(conexion -> {
            exigir(!catalogo.sedeRepetida(conexion, nombreLimpio, id), "Ya existe una sede con ese nombre.");
            if (id != 0) {
                exigir(capacidad >= sede(conexion, id).getPresentes(), "La capacidad es menor al aforo registrado.");
                exigir(capacidad >= actividad.capacidadClasesFuturas(conexion, id, LocalDateTime.now(reloj)),
                        "La capacidad es menor a los cupos de una clase programada.");
            }
            return catalogo.guardarSede(conexion, id, nombreLimpio, direccionLimpia, capacidad);
        });
    }

    public int guardarPlan(int id, String nombre, int meses, int precio, List<Integer> sedes) {
        String nombreLimpio = texto(nombre, "nombre");
        List<Integer> permitidas = new ArrayList<>();
        for (int sedeId : sedes) {
            if (!permitidas.contains(sedeId)) permitidas.add(sedeId);
        }
        exigir(meses >= 1 && meses <= 36, "La duración debe estar entre 1 y 36 meses.");
        exigir(precio > 0, "El precio debe ser positivo.");
        exigir(!permitidas.isEmpty(), "Elige al menos una sede permitida.");
        return cambiar(conexion -> {
            exigir(!catalogo.planRepetido(conexion, nombreLimpio, id), "Ya existe un plan con ese nombre.");
            if (id != 0) plan(conexion, id);
            for (int sedeId : permitidas) sede(conexion, sedeId);
            return catalogo.guardarPlan(conexion, id, nombreLimpio, meses, precio, permitidas);
        });
    }

    // --- Contratación, renovación y congelamiento ---

    public int contratar(int socioId, int planId, LocalDate inicio, EstrategiaPrecio estrategia) {
        exigir(inicio != null && inicio.getYear() >= 1900 && inicio.getYear() <= 9900, "Indica una fecha de inicio válida.");
        return registrarMembresia(socioId, planId, 0, inicio, estrategia);
    }

    public int renovar(int socioId, int planId, int anteriorId, EstrategiaPrecio estrategia) {
        exigir(anteriorId > 0, "Selecciona la membresía que quieres renovar.");
        return registrarMembresia(socioId, planId, anteriorId, null, estrategia);
    }

    private int registrarMembresia(int socioId, int planId, int anteriorId, LocalDate inicio,
                                   EstrategiaPrecio estrategia) {
        exigir(estrategia != null, "Selecciona una tarifa.");
        return cambiar(conexion -> {
            LocalDate hoy = LocalDate.now(reloj);
            exigir(socio(conexion, socioId).getEstado() == Socio.Estado.ACTIVO, "El socio está suspendido.");
            Plan plan = plan(conexion, planId);
            List<Membresia> historial = membresias.porSocio(conexion, socioId);
            Membresia anterior = anteriorId == 0 ? null : membresia(conexion, anteriorId);
            LocalDate fechaInicio = inicio;
            if (anterior != null) {
                fechaInicio = inicioRenovacion(anterior, historial, socioId, hoy);
            }
            LocalDate fin = fechaInicio.plusMonths(plan.getMeses()).minusDays(1);
            comprobarFechas(historial, fechaInicio, fin);

            EstrategiaPrecio.Contexto contexto =
                    new EstrategiaPrecio.Contexto(plan.getPrecio(),
                            historial.isEmpty(), anterior, hoy);
            int importe = estrategia.calcular(contexto);
            exigir(importe > 0, "La tarifa debe producir un precio positivo.");
            return membresias.guardar(conexion, socioId, planId,
                    fechaInicio, fin, importe, estrategia.nombre());
        });
    }

    private LocalDate inicioRenovacion(Membresia anterior, List<Membresia> historial, int socioId, LocalDate hoy) {
        exigir(anterior.getSocioId() == socioId && historial.get(0).getId() == anterior.getId(),
                "Solo se puede renovar la última membresía del socio.");
        Membresia.Vigencia vigencia = anterior.vigencia(hoy);
        exigir(vigencia != Membresia.Vigencia.CONGELADA && vigencia != Membresia.Vigencia.PROGRAMADA,
                "Para renovar, la membresía debe estar vigente o vencida.");
        if (anterior.getVencimiento().isBefore(hoy)) {
            return hoy;
        }
        return anterior.getVencimiento().plusDays(1);
    }

    private void comprobarFechas(List<Membresia> historial, LocalDate inicio, LocalDate fin) {
        for (Membresia existente : historial) {
            boolean fechasSeparadas = fin.isBefore(existente.getInicio())
                    || inicio.isAfter(existente.getVencimiento());
            exigir(fechasSeparadas, "Las fechas coinciden con otra membresía registrada.");
        }
    }

    public void eliminarMembresia(int id) {
        cambiar(conexion -> {
            membresia(conexion, id);
            exigir(!membresias.tieneDependencias(conexion, id)
                            && !actividad.tieneAsistenciaMembresia(conexion, id),
                    "No se puede deshacer la membresía porque ya tiene pagos, pausas o asistencias asociadas.");
            membresias.eliminar(conexion, id);
            return null;
        });
    }

    public void restaurarMembresia(Membresia registrada) {
        cambiar(conexion -> {
            exigir(membresias.porId(conexion, registrada.getId()) == null,
                    "La membresía ya existe y no se puede rehacer.");
            socio(conexion, registrada.getSocioId());
            plan(conexion, registrada.getPlanId());
            comprobarFechas(membresias.porSocio(conexion, registrada.getSocioId()),
                    registrada.getInicio(), registrada.getVencimiento());
            membresias.restaurar(conexion, registrada);
            return null;
        });
    }

    public void congelar(int id, int dias, String motivo) {
        String motivoLimpio = texto(motivo, "motivo");
        exigir(dias >= 1 && dias <= 90, "Indica entre 1 y 90 días.");
        cambiar(conexion -> {
            Membresia membresia = membresia(conexion, id);
            LocalDate hoy = LocalDate.now(reloj);
            exigir(membresia.vigenteEn(hoy), "Solo se puede congelar una membresía vigente sin pausa activa.");
            LocalDate nuevoVencimiento = membresia.getVencimiento().plusDays(dias);
            for (Membresia otra : membresias.porSocio(conexion, membresia.getSocioId())) {
                exigir(otra.getId() == id || !otra.getInicio().isAfter(membresia.getVencimiento())
                        || otra.getInicio().isAfter(nuevoVencimiento),
                        "La ampliación se cruzaría con una membresía ya programada.");
            }
            membresias.congelar(conexion, id, hoy, hoy.plusDays(dias), motivoLimpio, nuevoVencimiento);
            return null;
        });
    }

    public void reactivar(int id) {
        cambiar(conexion -> {
            Membresia membresia = membresia(conexion, id);
            LocalDate hoy = LocalDate.now(reloj);
            exigir(membresia.vigencia(hoy) == Membresia.Vigencia.CONGELADA, "La membresía no tiene una pausa activa.");
            Congelamiento pausa = membresia.getCongelamiento();
            long diasSinUsar = java.time.temporal.ChronoUnit.DAYS.between(hoy, pausa.getFin());
            membresias.reactivar(conexion, pausa, hoy, membresia.getVencimiento().minusDays(diasSinUsar));
            return null;
        });
    }

    // --- Pagos ---

    public int abonar(int membresiaId, int monto, Pago.Metodo metodo) {
        return guardarPago(membresiaId, monto, metodo, false);
    }

    public int pagarSaldo(int membresiaId, Pago.Metodo metodo) {
        return guardarPago(membresiaId, 0, metodo, true);
    }

    private int guardarPago(int id, int monto, Pago.Metodo metodo, boolean saldoCompleto) {
        exigir(metodo != null, "Selecciona un método de pago.");
        return cambiar(conexion -> {
            int saldo = membresia(conexion, id).saldo();
            int total = saldoCompleto ? saldo : monto;
            exigir(total > 0, "El importe debe ser positivo y la membresía debe tener saldo.");
            exigir(total <= saldo, "El abono supera el saldo de " + soles(saldo) + ".");
            return membresias.abonar(conexion, id, LocalDateTime.now(reloj), total, metodo);
        });
    }

    public void eliminarPago(Pago registrado) {
        cambiar(conexion -> {
            Pago actual = membresias.pago(conexion, registrado.getId());
            exigir(actual != null && actual.getMembresiaId() == registrado.getMembresiaId()
                            && actual.getFecha().equals(registrado.getFecha())
                            && actual.getMonto() == registrado.getMonto()
                            && actual.getMetodo() == registrado.getMetodo(),
                    "El abono cambió desde que se registró y no se puede deshacer.");
            membresias.eliminarPago(conexion, registrado.getId());
            return null;
        });
    }

    public void restaurarPago(Pago registrado) {
        cambiar(conexion -> {
            exigir(membresias.pago(conexion, registrado.getId()) == null,
                    "El abono ya existe y no se puede rehacer.");
            Membresia membresia = membresia(conexion, registrado.getMembresiaId());
            exigir(registrado.getMonto() > 0 && registrado.getMonto() <= membresia.saldo(),
                    "El saldo cambió y ya no permite rehacer este abono.");
            membresias.restaurarPago(conexion, registrado);
            return null;
        });
    }

    // --- Entrada, salida y aforo ---

    public void registrarEntrada(String dni, int sedeId) {
        String documento = dni(dni);
        cambiar(conexion -> {
            LocalDateTime ahora = LocalDateTime.now(reloj);
            LocalDate hoy = ahora.toLocalDate();
            Socio socio = socioPorDni(conexion, documento);
            exigir(socio.getEstado() == Socio.Estado.ACTIVO, "El socio está suspendido.");
            exigir(actividad.asistenciaAbierta(conexion, socio.getId()) == null,
                    "El socio ya tiene una entrada abierta.");
            List<Membresia> historial = membresias.porSocio(conexion, socio.getId());
            Membresia actual = membresiaDelDia(historial, hoy);
            exigir(actual.vigenteEn(hoy), "La membresía está congelada.");
            exigir(actual.abonoSuficiente(), "Se requiere haber pagado al menos 50 % de la membresía.");
            exigir(plan(conexion, actual.getPlanId()).getSedes().contains(sedeId), "El plan no permite ingresar a esta sede.");
            exigir(sede(conexion, sedeId).disponibles() > 0, "La sede alcanzó su aforo máximo.");
            actividad.entrada(conexion, socio.getId(), actual.getId(), sedeId, ahora);
            return null;
        });
    }

    private Membresia membresiaDelDia(List<Membresia> historial, LocalDate hoy) {
        exigir(!historial.isEmpty(), "El socio no tiene membresías registradas.");
        for (Membresia membresia : historial) {
            if (!hoy.isBefore(membresia.getInicio()) && !hoy.isAfter(membresia.getVencimiento())) {
                return membresia;
            }
        }
        if (historial.get(0).getInicio().isAfter(hoy)) {
            throw new DatosInvalidosException("La próxima membresía aún no empieza.");
        }
        throw new DatosInvalidosException("La membresía está vencida.");
    }

    public void registrarSalida(String dni, int sedeId) {
        String documento = dni(dni);
        cambiar(conexion -> {
            Socio socio = socioPorDni(conexion, documento);
            Asistencia abierta = actividad.asistenciaAbierta(conexion, socio.getId());
            exigir(abierta != null, "El socio no tiene una entrada abierta.");
            exigir(abierta.getSedeId() == sedeId, "Registra la salida en la sede donde se hizo la entrada.");
            LocalDateTime ahora = LocalDateTime.now(reloj);
            exigir(!ahora.isBefore(abierta.getEntrada()), "La hora del equipo es anterior a la entrada.");
            actividad.salida(conexion, abierta.getId(), ahora);
            return null;
        });
    }

    // --- Entrenadores, clases y reservas ---

    public int guardarEntrenador(Entrenador datos) {
        Entrenador limpio = new Entrenador(datos.getId(), texto(datos.getNombres(), "nombres"),
                texto(datos.getApellidos(), "apellidos"), texto(datos.getEspecialidad(), "especialidad"),
                texto(datos.getTelefono(), "teléfono"), texto(datos.getDisponibilidad(), "disponibilidad"));
        return cambiar(conexion -> {
            exigir(limpio.getId() == 0 || catalogo.existeEntrenador(conexion, limpio.getId()), "Entrenador no encontrado.");
            return catalogo.guardarEntrenador(conexion, limpio);
        });
    }

    public int guardarClase(String nombre, int entrenadorId, int sedeId, LocalDateTime horario, int capacidad) {
        String nombreLimpio = texto(nombre, "nombre");
        exigir(horario != null && horario.getYear() <= 9900 && horario.isAfter(LocalDateTime.now(reloj)),
                "Programa la clase para una fecha y hora futuras.");
        exigir(capacidad > 0, "Los cupos deben ser mayores que cero.");
        return cambiar(conexion -> {
            exigir(catalogo.existeEntrenador(conexion, entrenadorId), "Entrenador no encontrado.");
            exigir(capacidad <= sede(conexion, sedeId).getCapacidad(), "Los cupos exceden la capacidad de la sede.");
            return actividad.guardarClase(conexion, nombreLimpio, entrenadorId, sedeId, horario, capacidad);
        });
    }

    public void reservar(int claseId, int socioId) {
        cambiar(conexion -> {
            ClaseGrupal clase = clase(conexion, claseId);
            LocalDateTime ahora = LocalDateTime.now(reloj);
            exigir(clase.getHorario().isAfter(ahora), "La clase ya comenzó.");
            exigir(socio(conexion, socioId).getEstado() == Socio.Estado.ACTIVO, "El socio está suspendido.");
            exigir(!actividad.existeReserva(conexion, claseId, socioId), "El socio ya tiene un cupo reservado.");
            exigir(clase.disponibles() > 0, "La clase no tiene cupos disponibles.");
            actividad.reservar(conexion, claseId, socioId, ahora);
            return null;
        });
    }

    public void cancelarReserva(int claseId, int socioId) {
        cambiar(conexion -> {
            exigir(clase(conexion, claseId).getHorario().isAfter(LocalDateTime.now(reloj)), "La clase ya comenzó.");
            exigir(actividad.cancelar(conexion, claseId, socioId), "La reserva ya no existe.");
            return null;
        });
    }

    private Socio socio(Connection conexion, int id) throws SQLException {
        return encontrado(socios.porId(conexion, id), "Socio no encontrado.");
    }

    private Socio socioPorDni(Connection conexion, String dni) throws SQLException {
        return encontrado(socios.porDni(conexion, dni), "DNI no registrado.");
    }

    private Sede sede(Connection conexion, int id) throws SQLException {
        return encontrado(catalogo.sede(conexion, id), "Sede no encontrada.");
    }

    private Plan plan(Connection conexion, int id) throws SQLException {
        return encontrado(catalogo.plan(conexion, id), "Plan no encontrado.");
    }

    private Membresia membresia(Connection conexion, int id) throws SQLException {
        return encontrado(membresias.porId(conexion, id), "Membresía no encontrada.");
    }

    private ClaseGrupal clase(Connection conexion, int id) throws SQLException {
        return encontrado(actividad.clase(conexion, id), "Clase no encontrada.");
    }

    private <T> T encontrado(T dato, String mensaje) {
        exigir(dato != null, mensaje);
        return dato;
    }
}
