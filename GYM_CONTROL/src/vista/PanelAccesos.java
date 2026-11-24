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
    private final JTextField dni = new JTextField(10);
    private final JComboBox<Sede> sedes = new JComboBox<>();
    private final JLabel aforo = detalle("Selecciona una sede.");
    private final JLabel resultado = detalle(" ");
    private final Tabla<Asistencia> asistencias = new Tabla<>(Tabla.numero("ID", Asistencia::getId),
            Tabla.texto("Socio", Asistencia::getSocio), Tabla.texto("Sede", Asistencia::getSede),
            Tabla.numero("Membresía", Asistencia::getMembresiaId), Tabla.texto("Entrada", a -> hora(a.getEntrada())),
            Tabla.texto("Salida", a -> hora(a.getSalida())));

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

    private void registrar(boolean entrada) {
        Sede sede = (Sede) sedes.getSelectedItem();
        if (!disponible(sede != null, "Selecciona una sede.")) return;
        if (entrada) controlador.entrada(dni.getText(), sede.getId());
        else controlador.salida(dni.getText(), sede.getId());
    }

    private void mostrarAforo() {
        Sede sede = (Sede) sedes.getSelectedItem();
        aforo.setText(sede == null ? "Selecciona una sede."
                : "Aforo en " + sede.getNombre() + ": " + sede.getPresentes() + " de " + sede.getCapacidad()
                + " personas · " + sede.disponibles() + " espacios libres");
    }

    void resultado(String mensaje, boolean error) {
        resultado.setText(mensaje);
        resultado.setForeground(error ? new java.awt.Color(164, 43, 52) : Tema.VERDE);
    }

    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        Sede previa = (Sede) sedes.getSelectedItem();
        sedes.setModel(new DefaultComboBoxModel<>(estado.getSedes().toArray(Sede[]::new)));
        if (previa != null) {
            for (Sede sede : estado.getSedes()) {
                if (sede.getId() == previa.getId()) {
                    sedes.setSelectedItem(sede);
                    break;
                }
            }
        }
        mostrarAforo();
        asistencias.mostrar(estado.getAsistencias(), Asistencia::getId);
    }
}
