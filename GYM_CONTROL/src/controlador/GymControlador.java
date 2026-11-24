package controlador;

import excepciones.DatosInvalidosException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import modelo.Entrenador;
import modelo.EstadoGym;
import modelo.GymListener;
import modelo.GymModelo;
import modelo.Pago;
import modelo.Socio;
import modelo.Validador;
import modelo.comando.CambiarEstadoSocioComando;
import modelo.comando.Comando;
import modelo.comando.EditarSocioComando;
import modelo.comando.GestorComandos;
import modelo.comando.RegistrarSocioComando;
import modelo.comando.RegistrarMembresiaComando;
import modelo.comando.RegistrarPagoComando;
import modelo.estrategia.DescuentoNuevoSocio;
import modelo.estrategia.DescuentoRenovacion;
import modelo.estrategia.EstrategiaPrecio;
import modelo.estrategia.PrecioNormal;
import modelo.persistencia.ExportadorCsv;
import vista.VentanaPrincipal;

public final class GymControlador {
    private final GymModelo modelo;
    private final GestorComandos comandos = new GestorComandos();
    private VentanaPrincipal vista;
    private boolean trabajando;
    private boolean iniciado;

    private enum TipoTarea { INICIO, OPERACION, ACCESO, EXPORTACION }

    public GymControlador(GymModelo modelo) {
        this.modelo = modelo;
    }

    public void conectar(VentanaPrincipal vista) {
        this.vista = vista;
        for (GymListener observador : vista.observadores()) {
            GymListener enEdt = estado -> avisarEnPantalla(observador, estado);
            modelo.agregarListener(enEdt);
            vista.alCerrar(() -> modelo.eliminarListener(enEdt));
        }
    }

    private void avisarEnPantalla(GymListener observador, EstadoGym estado) {
        SwingUtilities.invokeLater(() -> observador.gymActualizado(estado));
    }

    public void iniciar(boolean demostracion) {
        tarea(() -> {
            modelo.inicializar(demostracion);
            modelo.actualizarVistas();
            return "Base SQLite abierta";
        }, TipoTarea.INICIO);
    }

    public void actualizar() {
        tarea(() -> {
            modelo.actualizarVistas();
            return "Datos actualizados";
        }, TipoTarea.OPERACION);
    }

    public List<EstrategiaPrecio> estrategias() {
        return List.of(new PrecioNormal(), new DescuentoNuevoSocio(), new DescuentoRenovacion());
    }

    // --- Socios e historial ---

    public void registrarSocio(String dni, String nombres, String apellidos, String telefono) {
        ejecutar(new RegistrarSocioComando(modelo, dni, nombres, apellidos, telefono));
    }

    public void editarSocio(Socio anterior, String dni, String nombres, String apellidos, String telefono) {
        Socio nuevo = new Socio(anterior.getId(), dni, nombres, apellidos, telefono, anterior.getFechaRegistro(), anterior.getEstado());
        ejecutar(new EditarSocioComando(modelo, anterior, nuevo));
    }

    public void estadoSocio(Socio anterior, Socio.Estado estado) {
        ejecutar(new CambiarEstadoSocioComando(modelo, anterior, estado));
    }

    private void ejecutar(Comando comando) {
        tarea(() -> {
            comandos.ejecutar(comando);
            return "Guardado: " + comando.descripcion();
        }, TipoTarea.OPERACION);
    }

    public void deshacer() {
        tarea(() -> "Deshecho: " + comandos.deshacer(), TipoTarea.OPERACION);
    }

    public void rehacer() {
        tarea(() -> "Rehecho: " + comandos.rehacer(), TipoTarea.OPERACION);
    }

    // --- Sedes, planes y membresías ---

    public void sede(int id, String nombre, String direccion, String capacidad) {
        guardar("Sede guardada", () -> modelo.guardarSede(id, nombre, direccion,
                Validador.entero(capacidad, "Capacidad")));
    }

    public void plan(int id, String nombre, String meses, String precio, List<Integer> sedes) {
        guardar("Plan guardado", () -> modelo.guardarPlan(id, nombre, Validador.entero(meses, "Duración"),
                Validador.centimos(precio), sedes));
    }

    public void contratar(int socio, int plan, String inicio, EstrategiaPrecio estrategia) {
        ejecutar(new RegistrarMembresiaComando(modelo,
                () -> modelo.contratar(socio, plan, Validador.fecha(inicio), estrategia)));
    }

    public void renovar(int socio, int plan, int anterior, EstrategiaPrecio estrategia) {
        ejecutar(new RegistrarMembresiaComando(modelo,
                () -> modelo.renovar(socio, plan, anterior, estrategia)));
    }

    public void congelar(int membresia, String dias, String motivo) {
        guardar("Congelamiento registrado", () -> modelo.congelar(membresia,
                Validador.entero(dias, "Días"), motivo));
    }

    public void reactivar(int membresia) {
        guardar("Membresía reactivada", () -> modelo.reactivar(membresia));
    }

    // --- Pagos, accesos y clases ---

    public void pagar(int membresia, String monto, Pago.Metodo metodo, boolean cancelarSaldo) {
        ejecutar(new RegistrarPagoComando(modelo, () -> cancelarSaldo
                ? modelo.pagarSaldo(membresia, metodo)
                : modelo.abonar(membresia, Validador.centimos(monto), metodo)));
    }

    public void entrada(String dni, int sede) {
        tarea(() -> {
            modelo.registrarEntrada(dni, sede);
            comandos.limpiar();
            return "Acceso permitido. Entrada registrada.";
        }, TipoTarea.ACCESO);
    }

    public void salida(String dni, int sede) {
        guardar("Salida registrada", () -> modelo.registrarSalida(dni, sede));
    }

    public void entrenador(Entrenador datos) {
        guardar("Entrenador guardado", () -> modelo.guardarEntrenador(datos));
    }

    public void clase(String nombre, int entrenador, int sede, String horario, String capacidad) {
        guardar("Clase programada", () -> modelo.guardarClase(nombre, entrenador, sede,
                Validador.horario(horario), Validador.entero(capacidad, "Cupos")));
    }

    public void reservar(int clase, int socio) {
        guardar("Cupo reservado", () -> modelo.reservar(clase, socio));
    }

    public void cancelarReserva(int clase, int socio) {
        guardar("Reserva cancelada", () -> modelo.cancelarReserva(clase, socio));
    }

    // --- Operaciones en segundo plano ---

    private void guardar(String mensaje, Runnable operacion) {
        tarea(() -> {
            operacion.run();
            comandos.limpiar();
            return mensaje;
        }, TipoTarea.OPERACION);
    }

    private void tarea(Callable<String> operacion, TipoTarea tipo) {
        if (!puedeIniciar(tipo)) return;
        iniciarTarea(tipo);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return operacion.call();
            }

            @Override protected void done() {
                terminar(this, tipo);
            }
        }.execute();
    }

    public void exportarAsistencias(Path archivo) {
        if (!puedeIniciar(TipoTarea.EXPORTACION)) return;
        iniciarTarea(TipoTarea.EXPORTACION);
        SwingWorker<String, Void> trabajador = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                ExportadorCsv.asistencias(archivo, modelo.consultar().getAsistencias(), this::setProgress);
                return "Reporte guardado en " + archivo.getFileName();
            }

            @Override protected void done() {
                terminar(this, TipoTarea.EXPORTACION);
            }
        };
        trabajador.addPropertyChangeListener(evento -> {
            if ("progress".equals(evento.getPropertyName())) vista.progreso((Integer) evento.getNewValue());
        });
        trabajador.execute();
    }

    private boolean puedeIniciar(TipoTarea tipo) {
        return !trabajando && (iniciado || tipo == TipoTarea.INICIO);
    }

    private void iniciarTarea(TipoTarea tipo) {
        trabajando = true;
        vista.ocupada(true, tipo == TipoTarea.EXPORTACION, false, false);
    }

    private void terminar(SwingWorker<String, Void> trabajador, TipoTarea tipo) {
        trabajando = false;
        String mensaje;
        boolean error = false;
        try {
            mensaje = trabajador.get();
            iniciado = true;
        } catch (InterruptedException fallo) {
            Thread.currentThread().interrupt();
            mensaje = "Se interrumpió la operación.";
            error = true;
        } catch (ExecutionException fallo) {
            Throwable causa = fallo.getCause();
            String prefijo = tipo == TipoTarea.ACCESO && causa instanceof DatosInvalidosException ? "Acceso denegado. " : "";
            mensaje = prefijo + (causa.getMessage() == null ? "La operación no se completó." : causa.getMessage());
            error = true;
        }
        vista.ocupada(false, false, comandos.puedeDeshacer(), comandos.puedeRehacer());
        if (tipo == TipoTarea.ACCESO) vista.resultadoAcceso(mensaje, error);
        vista.mensaje(mensaje, error);
        if (error && tipo == TipoTarea.INICIO) vista.dispose();
    }
}
