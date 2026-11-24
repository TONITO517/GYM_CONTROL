package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JSplitPane;
import modelo.EstadoGym;
import modelo.Membresia;
import modelo.Plan;
import modelo.Sede;
import modelo.Socio;
import modelo.Validador;
import modelo.Congelamiento;
import modelo.estrategia.EstrategiaPrecio;

final class PanelMembresias extends PanelModulo {
    private final GymControlador controlador;
    private final Tabla<Plan> planes = new Tabla<>(Tabla.numero("ID", Plan::getId), Tabla.texto("Plan", Plan::getNombre),
            Tabla.numero("Meses", Plan::getMeses), Tabla.texto("Precio base", plan -> Validador.soles(plan.getPrecio())),
            Tabla.texto("Sedes permitidas", this::nombresSedes));
    private final Tabla<Membresia> membresias = new Tabla<>(Tabla.numero("ID", Membresia::getId),
            Tabla.texto("Socio", Membresia::getSocio), Tabla.texto("Plan", Membresia::getPlan),
            Tabla.texto("Inicio", Membresia::getInicio), Tabla.texto("Vencimiento", Membresia::getVencimiento),
            Tabla.texto("Precio acordado", m -> Validador.soles(m.getPrecio())),
            Tabla.texto("Tarifa", Membresia::getTarifa), Tabla.texto("Estado", m -> m.vigencia(estado.hoy()).name().replace('_', ' ')));

    PanelMembresias(GymControlador controlador) {
        super("Planes y membresías", "Define precios y sedes. Consulta el historial y renueva o congela membresías.");
        this.controlador = controlador;
        accion("Nuevo plan", false, () -> formularioPlan(null));
        accion("Editar plan", false, () -> {
            Plan plan = seleccion(planes, "un plan");
            if (plan != null) formularioPlan(plan);
        });
        accion("Contratar", true, () -> formularioMembresia(null));
        accion("Renovar", false, () -> {
            Membresia membresia = seleccion(membresias, "una membresía");
            if (membresia != null) formularioMembresia(membresia);
        });
        accion("Congelar", false, this::congelar);
        accion("Reactivar", false, () -> {
            Membresia membresia = seleccion(membresias, "una membresía");
            if (membresia != null) controlador.reactivar(membresia.getId());
        });
        accion("Ver congelamiento", false, this::verCongelamiento);

        var catalogo = seccion("Planes disponibles", planes);
        catalogo.setPreferredSize(new Dimension(500, 160));
        JSplitPane dividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT, catalogo,
                seccion("Historial de membresías", membresias));
        dividido.setBorder(null);
        dividido.setContinuousLayout(true);
        dividido.setResizeWeight(0.32);
        dividido.setDividerLocation(176);
        add(dividido, BorderLayout.CENTER);
    }

    private String nombresSedes(Plan plan) {
        List<String> nombres = new ArrayList<>();
        for (Sede sede : sedesDelPlan(plan)) {
            nombres.add(sede.getNombre());
        }
        return String.join(", ", nombres);
    }

    private List<Sede> sedesDelPlan(Plan plan) {
        List<Sede> sedes = new ArrayList<>();
        if (plan != null) {
            for (Sede sede : estado.getSedes()) {
                if (plan.getSedes().contains(sede.getId())) sedes.add(sede);
            }
        }
        return sedes;
    }

    private void formularioPlan(Plan plan) {
        if (estado == null || !disponible(!estado.getSedes().isEmpty(), "Primero registra una sede.")) return;
        Formulario formulario = new Formulario();
        JTextField nombre = formulario.texto("Nombre", plan == null ? "" : plan.getNombre());
        JTextField meses = formulario.texto("Duración en meses", plan == null ? "1" : Integer.toString(plan.getMeses()));
        JTextField precio = formulario.texto("Precio en soles", plan == null ? "" : BigDecimal.valueOf(plan.getPrecio(), 2).toPlainString());
        List<Sede> iniciales = sedesDelPlan(plan);
        JList<Sede> sedes = formulario.multiples("Sedes permitidas (Ctrl para varias)", estado.getSedes(), iniciales);
        if (formulario.aceptar(this, plan == null ? "Registrar plan" : "Editar plan")) {
            List<Integer> seleccionadas = new ArrayList<>();
            for (Sede sede : sedes.getSelectedValuesList()) {
                seleccionadas.add(sede.getId());
            }
            controlador.plan(plan == null ? 0 : plan.getId(), nombre.getText(), meses.getText(),
                    precio.getText(), seleccionadas);
        }
    }

    private void formularioMembresia(Membresia anterior) {
        if (estado == null || !disponible(!estado.getPlanes().isEmpty(), "Primero registra un plan.")) return;
        List<Socio> socios = new ArrayList<>();
        for (Socio socio : estado.getSocios()) {
            boolean corresponde = anterior == null || socio.getId() == anterior.getSocioId();
            if (socio.getEstado() == Socio.Estado.ACTIVO && corresponde) socios.add(socio);
        }
        if (!disponible(!socios.isEmpty(), "Para contratar o renovar, debes seleccionar un socio activo.")) return;
        Formulario formulario = new Formulario();
        JComboBox<Socio> socio = formulario.elegir("Socio", socios, null);
        Plan planActual = planes.seleccion();
        if (anterior != null) {
            planActual = null;
            for (Plan disponible : estado.getPlanes()) {
                if (disponible.getId() == anterior.getPlanId()) {
                    planActual = disponible;
                    break;
                }
            }
        }
        JComboBox<Plan> plan = formulario.elegir("Plan", estado.getPlanes(), planActual);
        JComboBox<EstrategiaPrecio> tarifa = formulario.elegir("Tarifa", controlador.estrategias(), null);
        if (anterior == null) {
            JTextField inicio = formulario.texto("Fecha de inicio (AAAA-MM-DD)", estado.hoy().toString());
            if (formulario.aceptar(this, "Contratar membresía")) {
                Socio elegido = Formulario.elegido(socio);
                Plan planElegido = Formulario.elegido(plan);
                controlador.contratar(elegido.getId(), planElegido.getId(), inicio.getText(), Formulario.elegido(tarifa));
            }
        } else if (formulario.aceptar(this, "Renovar membresía #" + anterior.getId())) {
            controlador.renovar(anterior.getSocioId(), Formulario.elegido(plan).getId(), anterior.getId(), Formulario.elegido(tarifa));
        }
    }

    private void congelar() {
        Membresia membresia = seleccion(membresias, "una membresía");
        if (membresia == null) return;
        Formulario formulario = new Formulario();
        JTextField dias = formulario.texto("Días desde hoy", "7");
        JTextField motivo = formulario.texto("Motivo", "");
        if (formulario.aceptar(this, "Congelar membresía #" + membresia.getId())) {
            controlador.congelar(membresia.getId(), dias.getText(), motivo.getText());
        }
    }

    private void verCongelamiento() {
        Membresia membresia = seleccion(membresias, "una membresía");
        if (membresia == null) return;
        Congelamiento pausa = membresia.getCongelamiento();
        if (pausa == null) {
            aviso("La membresía no tiene congelamientos registrados.");
        } else {
            aviso("Último congelamiento\nInicio: " + pausa.getInicio() + "\nReactivación: " + pausa.getFin()
                    + "\nMotivo: " + pausa.getMotivo());
        }
    }

    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        planes.mostrar(estado.getPlanes(), Plan::getId);
        membresias.mostrar(estado.getMembresias(), Membresia::getId);
    }
}
