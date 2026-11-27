package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import modelo.ClaseGrupal.Entrenador;
import modelo.ClaseGrupal.ReservaClase;
import modelo.ClaseGrupal;
import modelo.EstadoGym;
import modelo.Sede;
import modelo.Socio;

final class PanelClases extends PanelModulo {
    private final GymControlador controlador;
    // Tablas y lista de participantes
    private final Tabla<Entrenador> entrenadores = new Tabla<>(Tabla.numero("ID", elemento -> elemento.getId()),
            Tabla.texto("Entrenador", elemento -> elemento.toString()), Tabla.texto("Especialidad", elemento -> elemento.getEspecialidad()),
            Tabla.texto("Teléfono", elemento -> elemento.getTelefono()), Tabla.texto("Disponibilidad", elemento -> elemento.getDisponibilidad()));
    private final Tabla<ClaseGrupal> clases = new Tabla<>(Tabla.texto("Clase", elemento -> elemento.getNombre()),
            Tabla.texto("Entrenador", elemento -> elemento.getEntrenador()), Tabla.texto("Sede", elemento -> elemento.getSede()),
            Tabla.texto("Horario", c -> c.getHorario().toString().replace('T', ' ')),
            Tabla.numero("Capacidad", elemento -> elemento.getCapacidad()), Tabla.numero("Libres", elemento -> elemento.disponibles()));
    private final DefaultListModel<ReservaClase> reservas = new DefaultListModel<>();
    private final JList<ReservaClase> participantes = new JList<>(reservas);

    // Constructor y eventos
    PanelClases(GymControlador controlador) {
        super("Entrenadores y clases", "Programa sesiones grupales y administra sus cupos.");
        this.controlador = controlador;
        accion("Nuevo entrenador", false, () -> entrenador(null));
        accion("Editar entrenador", false, () -> {
            Entrenador entrenador = seleccion(entrenadores, "un entrenador");
            if (entrenador != null) entrenador(entrenador);
        });
        accion("Nueva clase", true, () -> nuevaClase());
        accion("Reservar cupo", false, () -> reservar());
        accion("Cancelar reserva", false, () -> cancelar());

        javax.swing.JPanel listado = seccion("Entrenadores", entrenadores);
        listado.setPreferredSize(new Dimension(500, 180));
        javax.swing.JPanel lista = seccion("Participantes", new JScrollPane(participantes));
        lista.setPreferredSize(new Dimension(230, 120));
        JSplitPane detalle = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, seccion("Clases grupales", clases), lista);
        detalle.setResizeWeight(0.76);
        detalle.setBorder(null);
        JSplitPane dividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listado, detalle);
        dividido.setResizeWeight(0.40);
        dividido.setBorder(null);
        clases.alSeleccionar(() -> mostrarParticipantes());
        add(dividido, BorderLayout.CENTER);
    }

    // Formulario de entrenador
    private void entrenador(Entrenador entrenador) {
        Formulario formulario = new Formulario();
        JTextField nombres = formulario.texto("Nombres", entrenador == null ? "" : entrenador.getNombres());
        JTextField apellidos = formulario.texto("Apellidos", entrenador == null ? "" : entrenador.getApellidos());
        JTextField especialidad = formulario.texto("Especialidad", entrenador == null ? "" : entrenador.getEspecialidad());
        JTextField telefono = formulario.texto("Teléfono", entrenador == null ? "" : entrenador.getTelefono());
        JTextField horario = formulario.texto("Disponibilidad", entrenador == null ? "" : entrenador.getDisponibilidad());
        if (formulario.aceptar(this, entrenador == null ? "Registrar entrenador" : "Editar entrenador")) {
            controlador.entrenador(new Entrenador(entrenador == null ? 0 : entrenador.getId(), nombres.getText(),
                    apellidos.getText(), especialidad.getText(), telefono.getText(), horario.getText()));
        }
    }

    // Formulario de clase
    private void nuevaClase() {
        if (estado == null || !disponible(!estado.getEntrenadores().isEmpty() && !estado.getSedes().isEmpty(),
                "Primero registra una sede y un entrenador.")) return;
        Formulario formulario = new Formulario();
        JTextField nombre = formulario.texto("Clase", "");
        JComboBox<Entrenador> entrenador = formulario.elegir("Entrenador", estado.getEntrenadores(), entrenadores.seleccion());
        JComboBox<Sede> sede = formulario.elegir("Sede", estado.getSedes(), null);
        JTextField horario = formulario.texto("Fecha y hora (AAAA-MM-DD HH:mm)", estado.hoy().plusDays(1) + " 18:00");
        JTextField cupos = formulario.texto("Cupos", "");
        if (formulario.aceptar(this, "Programar clase grupal")) {
            controlador.clase(nombre.getText(), Formulario.elegido(entrenador).getId(), Formulario.elegido(sede).getId(), horario.getText(), cupos.getText());
        }
    }

    // Reserva y cancelación
    private void reservar() {
        ClaseGrupal clase = seleccion(clases, "una clase");
        if (clase == null) return;
        List<Socio> activos = new ArrayList<>();
        for (Socio socio : estado.getSocios()) {
            if (socio.getEstado() == Socio.Estado.ACTIVO) activos.add(socio);
        }
        if (!disponible(!activos.isEmpty(), "Primero registra o activa un socio.")) return;
        Formulario formulario = new Formulario();
        JComboBox<Socio> socio = formulario.elegir("Socio", activos, null);
        if (formulario.aceptar(this, "Reservar cupo en " + clase.getNombre())) {
            controlador.reservar(clase.getId(), Formulario.elegido(socio).getId());
        }
    }

    private void cancelar() {
        ReservaClase reserva = participantes.getSelectedValue();
        if (disponible(reserva != null, "Selecciona un participante de la clase.")) {
            controlador.cancelarReserva(reserva.getClaseId(), reserva.getSocioId());
        }
    }

    // Lista de participantes
    private void mostrarParticipantes() {
        ClaseGrupal clase = clases.seleccion();
        reservas.clear();
        if (clase == null) return;
        for (ReservaClase reserva : estado.getReservas()) {
            if (reserva.getClaseId() == clase.getId()) reservas.addElement(reserva);
        }
    }

    // Observer: actualización del panel
    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        entrenadores.mostrar(estado.getEntrenadores(), elemento -> elemento.getId());
        clases.mostrar(estado.getClases(), elemento -> elemento.getId());
        mostrarParticipantes();
    }
}
