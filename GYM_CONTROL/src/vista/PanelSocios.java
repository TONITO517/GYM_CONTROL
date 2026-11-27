package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import modelo.EstadoGym;
import modelo.Sede;
import modelo.Socio;

final class PanelSocios extends PanelModulo {
    private final GymControlador controlador;
    // Tabla de socios
    private final Tabla<Socio> tabla = new Tabla<>(Tabla.numero("ID", elemento -> elemento.getId()), Tabla.texto("DNI", elemento -> elemento.getDni()),
            Tabla.texto("Nombre completo", elemento -> elemento.nombreCompleto()), Tabla.texto("Teléfono", elemento -> elemento.getTelefono()),
            Tabla.texto("Fecha de registro", elemento -> elemento.getFechaRegistro()), Tabla.texto("Estado", elemento -> elemento.getEstado()));

    // Constructor y eventos
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
        javax.swing.JButton boton = accion(texto, false, () -> menu.doClick());
        menu.addPropertyChangeListener("enabled", evento -> boton.setEnabled(menu.isEnabled()));
        boton.setEnabled(menu.isEnabled());
    }

    // Cambio de estado del socio
    private void cambiarEstado(Socio.Estado estado) {
        Socio socio = seleccion(tabla, "un socio");
        if (socio != null && disponible(socio.getEstado() != estado, "El socio ya tiene ese estado.")) {
            controlador.estadoSocio(socio, estado);
        }
    }

    // Formulario de socios
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

    // Observer: actualización de socios
    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        tabla.mostrar(estado.getSocios(), elemento -> elemento.getId());
    }
}

// Panel relacionado: PanelSedes
final class PanelSedes extends PanelModulo {
    private final GymControlador controlador;
    private final Tabla<Sede> tabla = new Tabla<>(Tabla.numero("ID", elemento -> elemento.getId()), Tabla.texto("Sede", elemento -> elemento.getNombre()),
            Tabla.texto("Dirección", elemento -> elemento.getDireccion()), Tabla.numero("Capacidad", elemento -> elemento.getCapacidad()),
            Tabla.numero("Personas dentro", elemento -> elemento.getPresentes()), Tabla.numero("Espacios libres", elemento -> elemento.disponibles()));

    // Constructor del panel de sedes
    PanelSedes(GymControlador controlador) {
        super("Sedes", "Administra direcciones y capacidad. El aforo se actualiza con cada entrada y salida.");
        this.controlador = controlador;
        accion("Nueva sede", true, () -> formulario(null));
        accion("Editar sede", false, () -> {
            Sede sede = seleccion(tabla, "una sede");
            if (sede != null) formulario(sede);
        });
        add(tabla, BorderLayout.CENTER);
    }

    // Formulario de sedes
    private void formulario(Sede sede) {
        Formulario formulario = new Formulario();
        JTextField nombre = formulario.texto("Nombre", sede == null ? "" : sede.getNombre());
        JTextField direccion = formulario.texto("Dirección", sede == null ? "" : sede.getDireccion());
        JTextField capacidad = formulario.texto("Capacidad máxima", sede == null ? "" : Integer.toString(sede.getCapacidad()));
        if (formulario.aceptar(this, sede == null ? "Registrar sede" : "Editar sede")) {
            controlador.sede(sede == null ? 0 : sede.getId(), nombre.getText(), direccion.getText(), capacidad.getText());
        }
    }

    // Observer: actualización de sedes
    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        tabla.mostrar(estado.getSedes(), elemento -> elemento.getId());
    }
}
