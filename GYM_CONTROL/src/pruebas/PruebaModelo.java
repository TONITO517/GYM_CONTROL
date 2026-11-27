package pruebas;

import excepciones.DatosInvalidosException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import modelo.ClaseGrupal.Entrenador;
import modelo.GymListener;
import modelo.GymModelo;
import modelo.Membresia;
import modelo.Pago;
import modelo.Sede;
import modelo.Socio;
import modelo.comando.GestorComandos;
import modelo.comando.SocioComando;
import modelo.estrategia.DescuentoNuevoSocio;
import modelo.estrategia.DescuentoRenovacion;
import modelo.estrategia.PrecioNormal;
import modelo.persistencia.ExportadorCsv;

public final class PruebaModelo {
    private static int comprobaciones;

    private PruebaModelo() {}

    // Inicio de las pruebas
    public static void main(String[] args) throws Exception {
        System.out.println("=== PRUEBAS GYM CONTROL ===");
        Path carpeta = Files.createTempDirectory("prueba-gym-");
        try {
            probar(carpeta);
            System.out.println("TOTAL: " + comprobaciones + " comprobaciones aprobadas.");
        } finally {
            try (java.util.stream.Stream<Path> archivos = Files.list(carpeta)) {
                for (Path archivo : archivos.toList()) Files.delete(archivo);
            }
            Files.delete(carpeta);
        }
    }

    // Pruebas de las operaciones del modelo
    private static void probar(Path carpeta) throws Exception {
        Path archivo = carpeta.resolve("prueba.db");
        RelojPrueba reloj = new RelojPrueba();
        LocalDate hoy = LocalDate.now(reloj);
        GymModelo gym = new GymModelo(archivo, reloj);
        gym.inicializar(false);
        AtomicInteger avisosPrimero = new AtomicInteger();
        AtomicInteger avisosSegundo = new AtomicInteger();
        GymListener primero = estado -> avisosPrimero.incrementAndGet();
        GymListener segundo = estado -> avisosSegundo.incrementAndGet();
        gym.agregarObservador(primero);
        gym.agregarObservador(segundo);

        GestorComandos comandos = new GestorComandos();
        SocioComando alta = new SocioComando(gym, "00000001", "Ana", "Quispe", "900000001");
        comandos.ejecutar(alta);
        Socio ana = gym.consultar().getSocios().getFirst();
        verificar(ana.getDni().equals("00000001") && ana.getFechaRegistro().equals(hoy), "Registro de socio");
        verificar(avisosPrimero.get() == 1 && avisosSegundo.get() == 1, "Observer notifica a dos observadores");
        rechazar(() -> gym.registrarSocio("00000001", "Otra", "Persona", "900000002"), "DNI duplicado");
        verificar(avisosPrimero.get() == 1 && avisosSegundo.get() == 1, "Observer no notifica validaciones fallidas");
        gym.eliminarObservador(segundo);

        comandos.deshacer();
        verificar(gym.consultar().getSocios().isEmpty(),
                "Command deshace un registro sin relaciones");
        comandos.rehacer();
        verificar(gym.consultar().getSocios().getFirst().mismosDatos(ana),
                "Command rehace y conserva el ID");
        Socio anaEditada = new Socio(ana.getId(), ana.getDni(), ana.getNombres(), ana.getApellidos(), "911000001",
                ana.getFechaRegistro(), ana.getEstado());
        comandos.ejecutar(new SocioComando(gym, ana, anaEditada));
        verificar(gym.consultar().getSocios().getFirst().mismosDatos(anaEditada), "Command edita datos");
        comandos.deshacer();
        verificar(gym.consultar().getSocios().getFirst().mismosDatos(ana), "Command restaura los datos anteriores");
        comandos.ejecutar(new SocioComando(gym, ana, Socio.Estado.SUSPENDIDO));
        verificar(gym.consultar().activos() == 0 && !comandos.puedeRehacer(), "Acción nueva sustituye el historial de rehacer");
        comandos.deshacer();
        verificar(gym.consultar().activos() == 1 && avisosSegundo.get() == 1, "Estado restaurado y baja de Observer");

        Socio diego = gym.registrarSocio("00000002", "Diego", "López", "900000002");
        Socio luz = gym.registrarSocio("00000003", "Luz", "Prado", "900000003");
        int centro = gym.guardarSede(0, "Centro", "Dirección Centro", 1);
        int norte = gym.guardarSede(0, "Norte", "Dirección Norte", 2);
        gym.guardarSede(norte, "Norte", "Dirección actualizada", 2);
        boolean direccionActualizada = false;
        for (Sede sede : gym.consultar().getSedes()) {
            if (sede.getId() == norte) {
                direccionActualizada = sede.getDireccion().equals("Dirección actualizada");
            }
        }
        verificar(direccionActualizada, "Editar sede");
        int mensual = gym.guardarPlan(0, "Mensual", 1, 15000, List.of(centro));
        rechazar(() -> gym.guardarPlan(mensual, "Inválido", 2, 18000, List.of(centro, -1)), "Sede inválida en edición de plan");
        verificar(gym.consultar().getPlanes().getFirst().getNombre().equals("Mensual"), "El plan previo permanece tras validación");
        gym.guardarPlan(mensual, "Mensual Centro", 1, 15000, List.of(centro));
        verificar(gym.consultar().getPlanes().getFirst().getNombre().equals("Mensual Centro"), "Editar plan con sedes");

        int contrato = gym.contratar(ana.getId(), mensual, hoy.minusDays(25), new DescuentoNuevoSocio());
        verificar(contrato(gym, contrato).getPrecio() == 13500, "Strategy calcula 10 % para socio nuevo");
        LocalDate finOriginal = hoy.minusDays(25).plusMonths(1).minusDays(1);
        verificar(contrato(gym, contrato).getVencimiento().equals(finOriginal), "Fecha de vencimiento inclusive");
        verificar(contrato(gym, contrato).vigencia(hoy) == Membresia.Vigencia.POR_VENCER,
                "Estado por vencer calculado por fecha");
        rechazar(() -> gym.contratar(ana.getId(), mensual, hoy, new PrecioNormal()), "Bloquea contratos superpuestos");
        rechazar(() -> gym.contratar(ana.getId(), mensual, hoy.plusMonths(3), new DescuentoNuevoSocio()),
                "Bloquea descuento de primera compra cuando ya hay historial");

        gym.congelar(contrato, 4, "Viaje");
        verificar(contrato(gym, contrato).vigencia(hoy) == Membresia.Vigencia.CONGELADA
                && contrato(gym, contrato).getVencimiento().equals(finOriginal.plusDays(4)), "Congelamiento y extensión");
        rechazar(() -> gym.congelar(contrato, 2, "Duplicado"), "Congelamiento superpuesto");
        rechazar(() -> gym.registrarEntrada(ana.getDni(), centro), "Acceso bloqueado por congelamiento");
        reloj.diaSiguiente();
        gym.reactivar(contrato);
        verificar(contrato(gym, contrato).getVencimiento().equals(finOriginal.plusDays(1))
                && contrato(gym, contrato).vigenteEn(LocalDate.now(reloj)), "Reactivación conserva solo un día efectivo");

        int renovada = gym.renovar(ana.getId(), mensual, contrato, new DescuentoRenovacion());
        verificar(contrato(gym, renovada).getInicio().equals(finOriginal.plusDays(2))
                && contrato(gym, renovada).getPrecio() == 14250, "Renovación sin solape con 5 % de descuento");
        verificar(contrato(gym, renovada).vigencia(LocalDate.now(reloj)) == Membresia.Vigencia.PROGRAMADA,
                "Renovación futura aparece programada");
        rechazar(() -> gym.congelar(contrato, 2, "Viaje nuevo"), "Congelamiento no invade la próxima membresía");
        rechazar(() -> gym.renovar(ana.getId(), mensual, contrato, new PrecioNormal()), "No renueva dos veces el mismo periodo");

        gym.abonar(contrato, 5000, Pago.Metodo.YAPE);
        verificar(contrato(gym, contrato).saldo() == 8500 && contrato(gym, contrato).estadoPago().equals("Parcial"),
                "Abono parcial y saldo exacto");
        rechazar(() -> gym.registrarEntrada(ana.getDni(), centro), "Acceso bloqueado con menos de 50 % abonado");
        rechazar(() -> gym.abonar(contrato, 9000, Pago.Metodo.PLIN), "Rechaza abono superior al saldo");
        rechazar(() -> gym.abonar(contrato, -1, Pago.Metodo.PLIN), "Rechaza abono negativo");
        gym.abonar(contrato, 1750, Pago.Metodo.PLIN);
        verificar(contrato(gym, contrato).abonoSuficiente(), "Un abono de exactamente 50 % habilita el acceso");

        int otroContrato = gym.contratar(diego.getId(), mensual, LocalDate.now(reloj), new PrecioNormal());
        verificar(contrato(gym, otroContrato).getPrecio() == 15000, "Strategy permite tarifa normal");
        gym.pagarSaldo(otroContrato, Pago.Metodo.EFECTIVO);
        verificar(contrato(gym, otroContrato).saldo() == 0, "Pago completo del saldo");
        rechazar(() -> gym.registrarEntrada("99999999", centro), "DNI desconocido");
        rechazar(() -> gym.registrarEntrada(ana.getDni(), norte), "Sede fuera del plan");
        int vencida = gym.contratar(luz.getId(), mensual, hoy.minusMonths(3), new PrecioNormal());
        gym.pagarSaldo(vencida, Pago.Metodo.EFECTIVO);
        rechazar(() -> gym.registrarEntrada(luz.getDni(), centro), "Membresía vencida");
        rechazar(() -> gym.renovar(luz.getId(), mensual, vencida, new DescuentoRenovacion()),
                "Descuento anticipado requiere vigencia");
        int nuevoContrato = gym.renovar(luz.getId(), mensual, vencida, new PrecioNormal());
        verificar(contrato(gym, nuevoContrato).getInicio().equals(LocalDate.now(reloj)), "Renovación vencida empieza hoy");
        gym.reemplazarSocio(luz, luz.conEstado(Socio.Estado.SUSPENDIDO));
        rechazar(() -> gym.registrarEntrada(luz.getDni(), centro), "Socio suspendido");

        gym.registrarEntrada(ana.getDni(), centro);
        verificar(gym.consultar().presentes() == 1, "Entrada incrementa el aforo");
        rechazar(() -> gym.registrarEntrada(ana.getDni(), centro), "Entrada duplicada");
        rechazar(() -> gym.registrarEntrada(diego.getDni(), centro), "Rechaza por aforo completo");
        rechazar(() -> gym.registrarSalida(ana.getDni(), norte), "Salida en sede distinta");
        gym.registrarSalida(ana.getDni(), centro);
        verificar(gym.consultar().presentes() == 0, "Salida libera el aforo");
        rechazar(() -> gym.registrarSalida(ana.getDni(), centro), "Salida duplicada");
        rechazar(() -> comandos.deshacer(), "Command protege el socio con historial al deshacer su alta");

        int entrenador = gym.guardarEntrenador(new Entrenador(0, "Ana", "Pérez", "Yoga", "900000099", "Tarde"));
        gym.guardarEntrenador(new Entrenador(entrenador, "Ana", "Pérez", "Yoga", "900000099", "Mañana"));
        verificar(gym.consultar().getEntrenadores().getFirst().getDisponibilidad().equals("Mañana"), "Registro y edición de entrenador");
        LocalDateTime horario = LocalDateTime.now(reloj).plusDays(1);
        rechazar(() -> gym.guardarClase("Yoga", entrenador, centro, horario, 2), "Cupos mayores que la capacidad de sede");
        int clase = gym.guardarClase("Yoga", entrenador, centro, horario, 1);
        gym.reservar(clase, ana.getId());
        verificar(gym.consultar().getClases().getFirst().disponibles() == 0, "Reserva y cálculo de cupos");
        rechazar(() -> gym.reservar(clase, ana.getId()), "Reserva duplicada");
        rechazar(() -> gym.reservar(clase, diego.getId()), "Clase sin cupos");
        gym.cancelarReserva(clase, ana.getId());
        verificar(gym.consultar().getClases().getFirst().disponibles() == 1, "Cancelar reserva libera un cupo");

        probarCupoConcurrente(archivo, reloj, clase, ana.getId(), diego.getId());
        Path csv = carpeta.resolve("asistencias.csv");
        AtomicInteger progreso = new AtomicInteger();
        ExportadorCsv.asistencias(csv, gym.consultar().getAsistencias(), valor -> progreso.set(valor));
        verificar(progreso.get() == 100 && Files.readString(csv).contains("Ana Quispe"), "Exportación de asistencias CSV");
        GymModelo reabierto = new GymModelo(archivo, reloj);
        reabierto.inicializar(false);
        verificar(reabierto.consultar().getSocios().size() == 3 && reabierto.consultar().getPagos().size() == 4
                && reabierto.consultar().getReservas().size() == 1 && reabierto.consultar().getAsistencias().size() == 1,
                "Persistencia de datos tras reabrir SQLite");

        GymModelo demo = new GymModelo(carpeta.resolve("demo.db"), Clock.systemDefaultZone());
        demo.inicializar(true);
        demo.inicializar(true);
        verificar(demo.consultar().getSocios().size() == 5 && demo.consultar().getClases().size() == 2,
                "Los datos de demostración se insertan una sola vez");
    }

    // Prueba de reservas simultáneas
    private static void probarCupoConcurrente(Path archivo, Clock reloj, int clase, int primero, int segundo)
            throws Exception {
        GymModelo instancia1 = new GymModelo(archivo, reloj);
        GymModelo instancia2 = new GymModelo(archivo, reloj);
        CountDownLatch salida = new CountDownLatch(1);
        try (java.util.concurrent.ExecutorService ejecutor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Future<?> intento1 = ejecutor.submit(() -> competir(salida, instancia1, clase, primero));
            java.util.concurrent.Future<?> intento2 = ejecutor.submit(() -> competir(salida, instancia2, clase, segundo));
            salida.countDown();
            verificar(intento1.get() != intento2.get(), "Dos operaciones concurrentes no superan el último cupo");
        }
    }

    private static boolean competir(CountDownLatch salida, GymModelo gym, int clase, int socio)
            throws InterruptedException {
        salida.await();
        try {
            gym.reservar(clase, socio);
            return true;
        } catch (DatosInvalidosException error) {
            if (!error.getMessage().contains("cupos")) throw error;
            return false;
        }
    }

    private static Membresia contrato(GymModelo gym, int id) {
        for (Membresia membresia : gym.consultar().getMembresias()) {
            if (membresia.getId() == id) return membresia;
        }
        throw new AssertionError("No se encontró la membresía " + id);
    }

    // Comprobación de datos inválidos
    private static void rechazar(Runnable operacion, String prueba) {
        try {
            operacion.run();
            throw new AssertionError("Debía rechazar: " + prueba);
        } catch (DatosInvalidosException esperado) {
            verificar(true, prueba);
        }
    }

    // Verificación de resultados
    private static void verificar(boolean condicion, String prueba) {
        if (!condicion) throw new AssertionError(prueba);
        comprobaciones++;
        System.out.println("[OK] " + prueba);
    }

    private static final class RelojPrueba extends Clock {
        private Instant instante = Instant.parse("2026-03-27T10:00:00Z");

        void diaSiguiente() { instante = instante.plusSeconds(86_400); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return Clock.fixed(instante, zona); }
        @Override public Instant instant() { return instante; }
    }
}
