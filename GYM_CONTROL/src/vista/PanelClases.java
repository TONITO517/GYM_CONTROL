package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import modelo.ClaseGrupal;
import modelo.Entrenador;
import modelo.EstadoGym;
import modelo.ReservaClase;
import modelo.Socio;
import modelo.Sede;

final class PanelClases extends PanelModulo {
    private final GymControlador controlador;
    private final Tabla<Entrenador> entrenadores = new Tabla<>(Tabla.numero("ID", Entrenador::getId),
            Tabla.texto("Entrenador", Entrenador::toString), Tabla.texto("Especialidad", Entrenador::getEspecialidad),
            Tabla.texto("Teléfono", Entrenador::getTelefono), Tabla.texto("Disponibilidad", Entrenador::getDisponibilidad));
    private final Tabla<ClaseGrupal> clases = new Tabla<>(Tabla.texto("Clase", ClaseGrupal::getNombre),
            Tabla.texto("Entrenador", ClaseGrupal::getEntrenador), Tabla.texto("Sede", ClaseGrupal::getSede),
            Tabla.texto("Horario", c -> c.getHorario().toString().replace('T', ' ')),
            Tabla.numero("Capacidad", ClaseGrupal::getCapacidad), Tabla.numero("Libres", ClaseGrupal::disponibles));
    private final DefaultListModel<ReservaClase> reservas = new DefaultListModel<>();
    private final JList<ReservaClase> participantes = new JList<>(reservas);

    PanelClases(GymControlador controlador) {
        super("Entrenadores y clases", "Programa sesiones grupales y administra sus cupos.");
        this.controlador = controlador;
        accion("Nuevo entrenador", false, () -> entrenador(null));
        accion("Editar entrenador", false, () -> {
            Entrenador entrenador = seleccion(entrenadores, "un entrenador");
            if (entrenador != null) entrenador(entrenador);
        });
        accion("Nueva clase", true, this::nuevaClase);
        accion("Reservar cupo", false, this::reservar);
        accion("Cancelar reserva", false, this::cancelar);

        var listado = seccion("Entrenadores", entrenadores);
        listado.setPreferredSize(new Dimension(500, 180));
        var lista = seccion("Participantes", new JScrollPane(participantes));
        lista.setPreferredSize(new Dimension(230, 120));
        JSplitPane detalle = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, seccion("Clases grupales", clases), lista);
        detalle.setResizeWeight(0.76);
        detalle.setBorder(null);
        JSplitPane dividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listado, detalle);
        dividido.setResizeWeight(0.40);
        dividido.setBorder(null);
        clases.alSeleccionar(this::mostrarParticipantes);
        add(dividido, BorderLayout.CENTER);
    }

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

    private void mostrarParticipantes() {
        ClaseGrupal clase = clases.seleccion();
        reservas.clear();
        if (clase == null) return;
        for (ReservaClase reserva : estado.getReservas()) {
            if (reserva.getClaseId() == clase.getId()) reservas.addElement(reserva);
        }
    }

    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        entrenadores.mostrar(estado.getEntrenadores(), Entrenador::getId);
        clases.mostrar(estado.getClases(), ClaseGrupal::getId);
        mostrarParticipantes();
    }
}
