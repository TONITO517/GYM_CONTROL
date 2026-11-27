package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import modelo.GymListener;

public final class VentanaPrincipal extends JFrame {
    // Componentes principales
    private final GymControlador controlador;
    private final JTabbedPane pestanas = new JTabbedPane();
    private final JLabel mensaje = PanelModulo.etiqueta("Abriendo base de datos…", 13, false, PanelModulo.MUTED);
    private final JProgressBar progreso = new JProgressBar(0, 100);
    private final JMenuItem deshacer = new JMenuItem("Deshacer");
    private final JMenuItem rehacer = new JMenuItem("Rehacer");
    private final PanelAccesos accesos;
    private final List<GymListener> observadores;
    private final Timer reloj;

    // Constructor de JFrame
    public VentanaPrincipal(GymControlador controlador) {
        super("GYM CONTROL");
        this.controlador = controlador;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(950, 600));
        setSize(1110, 735);
        setLocationRelativeTo(null);
        setJMenuBar(menu());

        JPanel marca = new JPanel(new BorderLayout());
        marca.setBackground(PanelModulo.TINTA);
        marca.setBorder(BorderFactory.createEmptyBorder(15, 26, 15, 26));
        javax.swing.JLabel logo = PanelModulo.etiqueta("GYM CONTROL", 25, true, PanelModulo.LIMA);
        marca.add(logo, BorderLayout.WEST);
        marca.add(PanelModulo.etiqueta("Gestión de gimnasio", 13, false, Color.WHITE), BorderLayout.EAST);
        add(marca, BorderLayout.NORTH);

        // Creación de paneles observadores
        PanelResumen resumen = new PanelResumen(controlador, () -> exportar());
        PanelSocios socios = new PanelSocios(controlador, deshacer, rehacer);
        PanelSedes sedes = new PanelSedes(controlador);
        PanelMembresias membresias = new PanelMembresias(controlador);
        PanelPagos pagos = new PanelPagos(controlador);
        accesos = new PanelAccesos(controlador);
        PanelClases clases = new PanelClases(controlador);
        observadores = List.of(resumen, socios, sedes, membresias, pagos, accesos, clases);
        // Pestañas de la aplicación
        pestanas.addTab("Inicio", resumen);
        pestanas.addTab("Socios", socios);
        pestanas.addTab("Sedes", sedes);
        pestanas.addTab("Planes y membresías", membresias);
        pestanas.addTab("Pagos", pagos);
        pestanas.addTab("Accesos y asistencias", accesos);
        pestanas.addTab("Entrenadores y clases", clases);
        add(pestanas, BorderLayout.CENTER);

        // Barra de estado y progreso
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(Color.WHITE);
        pie.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, PanelModulo.LINEA),
                BorderFactory.createEmptyBorder(6, 26, 6, 26)));
        progreso.setStringPainted(true);
        progreso.setVisible(false);
        JPanel tareas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        tareas.setOpaque(false);
        tareas.add(progreso);
        pie.add(mensaje, BorderLayout.CENTER);
        pie.add(tareas, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);
        // Actualiza las fechas si la ventana sigue abierta al pasar al día siguiente.
        reloj = new Timer(60_000, evento -> {
            if (!java.time.LocalDate.now().equals(resumen.estado == null ? null : resumen.estado.hoy())) {
                controlador.actualizar();
            }
        });
        reloj.start();
        alCerrar(() -> reloj.stop());
    }

    public List<GymListener> observadores() { return observadores; }

    public void alCerrar(Runnable accion) {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent evento) { accion.run(); }
        });
    }

    // Menú y atajos de teclado
    private JMenuBar menu() {
        JMenuBar barra = new JMenuBar();
        JMenu archivo = new JMenu("Archivo");
        JMenu editar = new JMenu("Editar");
        JMenu ayuda = new JMenu("Ayuda");
        JMenuItem reporte = new JMenuItem("Exportar asistencias…");
        JMenuItem cerrar = new JMenuItem("Salir");
        JMenuItem reglas = new JMenuItem("Reglas del avance");
        reporte.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
        deshacer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        rehacer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        reporte.addActionListener(evento -> exportar());
        cerrar.addActionListener(evento -> dispose());
        deshacer.addActionListener(evento -> controlador.deshacer());
        rehacer.addActionListener(evento -> controlador.rehacer());
        reglas.addActionListener(evento -> JOptionPane.showMessageDialog(this, """
                El ingreso requiere una membresía vigente y al menos 50 % pagado.
                Una renovación empieza al día siguiente del vencimiento, o hoy si ya venció.
                Deshacer y rehacer se aplican a socios, membresías y abonos.
                Las operaciones no reversibles inician un historial nuevo.
                """, "Reglas del avance", JOptionPane.INFORMATION_MESSAGE));
        archivo.add(reporte);
        archivo.add(cerrar);
        editar.add(deshacer);
        editar.add(rehacer);
        ayuda.add(reglas);
        barra.add(archivo);
        barra.add(editar);
        barra.add(ayuda);
        return barra;
    }

    // JFileChooser: exportación de archivos
    private void exportar() {
        JFileChooser elegir = new JFileChooser();
        elegir.setDialogTitle("Exportar historial de asistencias");
        elegir.setFileFilter(new FileNameExtensionFilter("Archivo CSV", "csv"));
        elegir.setSelectedFile(new File("asistencias.csv"));
        if (elegir.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File destino = elegir.getSelectedFile();
        if (!destino.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            destino = new File(destino.getPath() + ".csv");
        }
        if (destino.exists() && JOptionPane.showConfirmDialog(this, "¿Reemplazar el archivo existente?",
                "Exportar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        controlador.exportarAsistencias(destino.toPath());
    }

    // Estado de la interfaz durante las tareas
    public void ocupada(boolean ocupada, boolean exportando, boolean puedeDeshacer, boolean puedeRehacer) {
        setEnabled(!ocupada);
        deshacer.setEnabled(puedeDeshacer);
        rehacer.setEnabled(puedeRehacer);
        progreso.setValue(0);
        progreso.setVisible(exportando);
        if (ocupada) mensaje.setText(exportando ? "Generando reporte..." : "Procesando...");
    }

    public void progreso(int porcentaje) { progreso.setValue(porcentaje); }

    public void resultadoAcceso(String texto, boolean error) { accesos.resultado(texto, error); }

    public void mensaje(String texto, boolean error) {
        mensaje.setText(texto);
        if (error) JOptionPane.showMessageDialog(this, texto, "GYM CONTROL", JOptionPane.WARNING_MESSAGE);
    }
}
