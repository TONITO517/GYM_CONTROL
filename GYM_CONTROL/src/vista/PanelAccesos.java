package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import modelo.Asistencia;
import modelo.EstadoGym;
import modelo.Sede;

final class PanelAccesos extends PanelModulo {
    private final GymControlador controlador;
    // Componentes de acceso
    private final JTextField dni = new JTextField(10);
    private final JComboBox<Sede> sedes = new JComboBox<>();
    private final JLabel aforo = detalle("Selecciona una sede.");
    private final JLabel resultado = detalle(" ");
    private final Tabla<Asistencia> asistencias = new Tabla<>(Tabla.numero("ID", elemento -> elemento.getId()),
            Tabla.texto("Socio", elemento -> elemento.getSocio()), Tabla.texto("Sede", elemento -> elemento.getSede()),
            Tabla.numero("Membresía", elemento -> elemento.getMembresiaId()), Tabla.texto("Entrada", a -> hora(a.getEntrada())),
            Tabla.texto("Salida", a -> hora(a.getSalida())));

    // Constructor y eventos
    PanelAccesos(GymControlador controlador) {
        super("Accesos y asistencias", "Valida la entrada por DNI. Registra la salida en la misma sede.");
        this.controlador = controlador;
        acciones.add(detalle("DNI"));
        acciones.add(dni);
        acciones.add(detalle("Sede"));
        acciones.add(sedes);
        sedes.addActionListener(evento -> mostrarAforo());
        accion("Registrar entrada", true, () -> registrar(true));
        accion("Registrar salida", false, () -> registrar(false));

        JPanel cuerpo = new JPanel(new BorderLayout(0, 14));
        cuerpo.setOpaque(false);
        JPanel estado = new JPanel(new GridLayout(2, 1, 0, 4));
        estado.setOpaque(false);
        estado.add(aforo);
        estado.add(resultado);
        cuerpo.add(estado, BorderLayout.NORTH);
        cuerpo.add(seccion("Historial de asistencias", asistencias), BorderLayout.CENTER);
        add(cuerpo, BorderLayout.CENTER);
    }

    private static String hora(LocalDateTime fecha) {
        return fecha == null ? "Dentro de la sede" : fecha.withNano(0).toString().replace('T', ' ');
    }

    // Registro de entrada o salida
    private void registrar(boolean entrada) {
        Sede sede = (Sede) sedes.getSelectedItem();
        if (!disponible(sede != null, "Selecciona una sede.")) return;
        if (entrada) controlador.entrada(dni.getText(), sede.getId());
        else controlador.salida(dni.getText(), sede.getId());
    }

    // Visualización del aforo
    private void mostrarAforo() {
        Sede sede = (Sede) sedes.getSelectedItem();
        aforo.setText(sede == null ? "Selecciona una sede."
                : "Aforo en " + sede.getNombre() + ": " + sede.getPresentes() + " de " + sede.getCapacidad()
                + " personas · " + sede.disponibles() + " espacios libres");
    }

    void resultado(String mensaje, boolean error) {
        resultado.setText(mensaje);
        resultado.setForeground(error ? new java.awt.Color(164, 43, 52) : PanelModulo.VERDE);
    }

    // Observer: actualización del panel
    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        Sede previa = (Sede) sedes.getSelectedItem();
        sedes.setModel(new DefaultComboBoxModel<>(estado.getSedes().toArray(new Sede[0])));
        if (previa != null) {
            for (Sede sede : estado.getSedes()) {
                if (sede.getId() == previa.getId()) {
                    sedes.setSelectedItem(sede);
                    break;
                }
            }
        }
        mostrarAforo();
        asistencias.mostrar(estado.getAsistencias(), elemento -> elemento.getId());
    }
}
