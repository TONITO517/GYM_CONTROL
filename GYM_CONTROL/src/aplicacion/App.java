package aplicacion;

import controlador.GymControlador;
import java.awt.Font;
import java.nio.file.Path;
import java.time.Clock;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import modelo.GymModelo;
import vista.VentanaPrincipal;

public final class App {
    private App() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::abrirVentana);
    }

    private static void abrirVentana() {
        configurarInterfaz();
        Path archivo = Path.of(System.getProperty("gym.db", "gym_control.db"));
        GymModelo modelo = new GymModelo(archivo, Clock.systemDefaultZone());
        GymControlador controlador = new GymControlador(modelo);
        VentanaPrincipal ventana = new VentanaPrincipal(controlador);
        controlador.conectar(ventana);
        ventana.setVisible(true);
        controlador.iniciar(!Boolean.getBoolean("gym.sinDemo"));
    }

    private static void configurarInterfaz() {
        FontUIResource fuente = new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 13);
        for (Object clave : UIManager.getLookAndFeelDefaults().keySet()) {
            if (clave.toString().endsWith(".font")) UIManager.put(clave, fuente);
        }
        UIManager.put("OptionPane.okButtonText", "Aceptar");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.yesButtonText", "Sí");
        UIManager.put("OptionPane.noButtonText", "No");
    }
}
