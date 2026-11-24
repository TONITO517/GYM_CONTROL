package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import javax.swing.JTextField;
import javax.swing.JMenuItem;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import modelo.EstadoGym;
import modelo.Socio;

final class PanelSocios extends PanelModulo {
    private final GymControlador controlador;
    private final Tabla<Socio> tabla = new Tabla<>(Tabla.numero("ID", Socio::getId), Tabla.texto("DNI", Socio::getDni),
            Tabla.texto("Nombre completo", Socio::nombreCompleto), Tabla.texto("Teléfono", Socio::getTelefono),
            Tabla.texto("Fecha de registro", Socio::getFechaRegistro), Tabla.texto("Estado", Socio::getEstado));

    PanelSocios(GymControlador controlador, JMenuItem deshacer, JMenuItem rehacer) {
        super("Socios", "Registra socios, busca por DNI o nombre y administra su estado.");
        this.controlador = controlador;
        JTextField buscar = new JTextField(16);
        acciones.add(detalle("Buscar"));
        acciones.add(buscar);
        buscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent evento) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent evento) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent evento) { filtrar(); }
            private void filtrar() { tabla.buscar(buscar.getText(), 1, 2); }
        });
        accion("Nuevo socio", true, () -> formulario(null));
        accion("Editar", false, () -> {
            Socio socio = seleccion(tabla, "un socio");
            if (socio != null) formulario(socio);
        });
        accion("Suspender", false, () -> cambiarEstado(Socio.Estado.SUSPENDIDO));
        accion("Activar", false, () -> cambiarEstado(Socio.Estado.ACTIVO));
        vincular("Deshacer", deshacer);
        vincular("Rehacer", rehacer);
        add(tabla, BorderLayout.CENTER);
    }

    private void vincular(String texto, JMenuItem menu) {
        var boton = accion(texto, false, menu::doClick);
        menu.addPropertyChangeListener("enabled", evento -> boton.setEnabled(menu.isEnabled()));
        boton.setEnabled(menu.isEnabled());
    }

    private void cambiarEstado(Socio.Estado estado) {
        Socio socio = seleccion(tabla, "un socio");
        if (socio != null && disponible(socio.getEstado() != estado, "El socio ya tiene ese estado.")) {
            controlador.estadoSocio(socio, estado);
        }
    }

    private void formulario(Socio socio) {
        Formulario formulario = new Formulario();
        JTextField dni = formulario.texto("DNI", socio == null ? "" : socio.getDni());
        JTextField nombres = formulario.texto("Nombres", socio == null ? "" : socio.getNombres());
        JTextField apellidos = formulario.texto("Apellidos", socio == null ? "" : socio.getApellidos());
        JTextField telefono = formulario.texto("Teléfono", socio == null ? "" : socio.getTelefono());
        if (formulario.aceptar(this, socio == null ? "Registrar socio" : "Editar socio")) {
            if (socio == null) controlador.registrarSocio(dni.getText(), nombres.getText(), apellidos.getText(), telefono.getText());
            else controlador.editarSocio(socio, dni.getText(), nombres.getText(), apellidos.getText(), telefono.getText());
        }
    }

    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        tabla.mostrar(estado.getSocios(), Socio::getId);
    }
}
