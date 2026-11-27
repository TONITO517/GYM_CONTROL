package controlador;

import excepciones.DatosInvalidosException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import modelo.ClaseGrupal.Entrenador;
import modelo.EstadoGym;
import modelo.GymListener;
import modelo.GymModelo;
import modelo.Pago;
import modelo.Socio;
import modelo.Validador;
import modelo.comando.Comando;
import modelo.comando.GestorComandos;
import modelo.comando.MembresiaComando;
import modelo.comando.PagoComando;
import modelo.comando.SocioComando;
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

    private interface Tarea {
        String ejecutar();
    }

    private enum TipoTarea { INICIO, OPERACION, ACCESO, EXPORTACION }

    // Constructor
    public GymControlador(GymModelo modelo) {
        this.modelo = modelo;
    }

    // Registro de observadores
    public void conectar(VentanaPrincipal vista) {
        this.vista = vista;
        for (GymListener observador : vista.observadores()) {
            GymListener enEdt = estado -> avisarEnPantalla(observador, estado);
            modelo.agregarObservador(enEdt);
            vista.alCerrar(() -> modelo.eliminarObservador(enEdt));
        }
    }

    // Notificación en el EDT
    private void avisarEnPantalla(GymListener observador, EstadoGym estado) {
        SwingUtilities.invokeLater(() -> observador.gymActualizado(estado));
    }

    // Carga inicial de datos
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

    // Strategy: lista de tarifas
    public List<EstrategiaPrecio> estrategias() {
        List<EstrategiaPrecio> estrategias = new java.util.ArrayList<>();
        estrategias.add(new PrecioNormal());
        estrategias.add(new DescuentoNuevoSocio());
        estrategias.add(new DescuentoRenovacion());
        return estrategias;
    }

    // --- Socios e historial ---

    public void registrarSocio(String dni, String nombres, String apellidos, String telefono) {
        ejecutar(new SocioComando(modelo, dni, nombres, apellidos, telefono));
    }

    public void editarSocio(Socio anterior, String dni, String nombres, String apellidos, String telefono) {
        Socio nuevo = new Socio(anterior.getId(), dni, nombres, apellidos, telefono, anterior.getFechaRegistro(), anterior.getEstado());
        ejecutar(new SocioComando(modelo, anterior, nuevo));
    }

    public void estadoSocio(Socio anterior, Socio.Estado estado) {
        ejecutar(new SocioComando(modelo, anterior, estado));
    }

    // Command: ejecución e historial
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
        ejecutar(new MembresiaComando(modelo, socio, plan, inicio, estrategia));
    }

    public void renovar(int socio, int plan, int anterior, EstrategiaPrecio estrategia) {
        ejecutar(new MembresiaComando(modelo, socio, plan, anterior, estrategia));
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
        ejecutar(new PagoComando(modelo, membresia, monto, metodo, cancelarSaldo));
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

    // SwingWorker: operaciones en segundo plano
    private void tarea(Tarea operacion, TipoTarea tipo) {
        if (!puedeIniciar(tipo)) return;
        iniciarTarea(tipo);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return operacion.ejecutar();
            }

            @Override protected void done() {
                terminar(this, tipo);
            }
        }.execute();
    }

    // SwingWorker: exportación CSV
    public void exportarAsistencias(Path archivo) {
        if (!puedeIniciar(TipoTarea.EXPORTACION)) return;
        iniciarTarea(TipoTarea.EXPORTACION);
        SwingWorker<String, Void> trabajador = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                ExportadorCsv.asistencias(archivo, modelo.consultar().getAsistencias(), valor -> setProgress(valor));
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

    // Resultado de la tarea y manejo de errores
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
