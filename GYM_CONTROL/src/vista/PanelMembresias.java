package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import modelo.EstadoGym;
import modelo.Membresia.Congelamiento;
import modelo.Membresia;
import modelo.Pago;
import modelo.Plan;
import modelo.Sede;
import modelo.Socio;
import modelo.Validador;
import modelo.estrategia.EstrategiaPrecio;

final class PanelMembresias extends PanelModulo {
    private final GymControlador controlador;
    // Tablas de planes y membresías
    private final Tabla<Plan> planes = new Tabla<>(Tabla.numero("ID", elemento -> elemento.getId()), Tabla.texto("Plan", elemento -> elemento.getNombre()),
            Tabla.numero("Meses", elemento -> elemento.getMeses()), Tabla.texto("Precio base", plan -> Validador.soles(plan.getPrecio())),
            Tabla.texto("Sedes permitidas", plan -> nombresSedes(plan)));
    private final Tabla<Membresia> membresias = new Tabla<>(Tabla.numero("ID", elemento -> elemento.getId()),
            Tabla.texto("Socio", elemento -> elemento.getSocio()), Tabla.texto("Plan", elemento -> elemento.getPlan()),
            Tabla.texto("Inicio", elemento -> elemento.getInicio()), Tabla.texto("Vencimiento", elemento -> elemento.getVencimiento()),
            Tabla.texto("Precio acordado", m -> Validador.soles(m.getPrecio())),
            Tabla.texto("Tarifa", elemento -> elemento.getTarifa()), Tabla.texto("Estado", m -> m.vigencia(estado.hoy()).name().replace('_', ' ')));

    // Constructor y eventos
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
        accion("Congelar", false, () -> congelar());
        accion("Reactivar", false, () -> {
            Membresia membresia = seleccion(membresias, "una membresía");
            if (membresia != null) controlador.reactivar(membresia.getId());
        });
        accion("Ver congelamiento", false, () -> verCongelamiento());

        javax.swing.JPanel catalogo = seccion("Planes disponibles", planes);
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

    // Formulario de planes
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

    // Contratación y renovación
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
        // Strategy: selección de tarifa
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

    // Congelamiento de membresías
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

    // Observer: actualización de membresías
    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        planes.mostrar(estado.getPlanes(), elemento -> elemento.getId());
        membresias.mostrar(estado.getMembresias(), elemento -> elemento.getId());
    }
}

// Panel relacionado: PanelPagos
final class PanelPagos extends PanelModulo {
    private final GymControlador controlador;
    private final Tabla<Membresia> saldos = new Tabla<>(Tabla.numero("Membresía", elemento -> elemento.getId()),
            Tabla.texto("Socio", elemento -> elemento.getSocio()), Tabla.texto("Plan", elemento -> elemento.getPlan()),
            Tabla.texto("Importe", m -> Validador.soles(m.getPrecio())),
            Tabla.texto("Total pagado", m -> Validador.soles(m.getPagado())),
            Tabla.texto("Saldo", m -> Validador.soles(m.saldo())), Tabla.texto("Estado", elemento -> elemento.estadoPago()));
    private final Tabla<Pago> historial = new Tabla<>(Tabla.numero("ID", elemento -> elemento.getId()),
            Tabla.texto("Fecha y hora", p -> p.getFecha().withNano(0).toString().replace('T', ' ')),
            Tabla.texto("Monto", p -> Validador.soles(p.getMonto())), Tabla.texto("Método", elemento -> elemento.getMetodo()));

    // Constructor del panel de pagos
    PanelPagos(GymControlador controlador) {
        super("Pagos", "Registra abonos y consulta el historial. El acceso exige un mínimo pagado del 50 %.");
        this.controlador = controlador;
        accion("Registrar abono", true, () -> pagar(false));
        accion("Pagar saldo completo", false, () -> pagar(true));
        javax.swing.JPanel resumen = seccion("Saldos por membresía", saldos);
        resumen.setPreferredSize(new Dimension(500, 220));
        JSplitPane dividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resumen,
                seccion("Abonos de la membresía seleccionada", historial));
        dividido.setBorder(null);
        dividido.setResizeWeight(0.58);
        saldos.alSeleccionar(() -> mostrarHistorial());
        add(dividido, BorderLayout.CENTER);
    }

    // Registro de pagos
    private void pagar(boolean saldoCompleto) {
        Membresia membresia = seleccion(saldos, "una membresía");
        if (membresia == null || !disponible(membresia.saldo() > 0, "La membresía ya está pagada.")) return;
        Formulario formulario = new Formulario();
        JComboBox<Pago.Metodo> metodo = formulario.elegir("Método de pago", Arrays.asList(Pago.Metodo.values()), null);
        String titulo = "#" + membresia.getId() + " · " + membresia.getSocio() + " · saldo " + Validador.soles(membresia.saldo());
        if (saldoCompleto) {
            if (formulario.aceptar(this, "Pagar saldo completo · " + titulo)) {
                controlador.pagar(membresia.getId(), "", Formulario.elegido(metodo), true);
            }
        } else {
            JTextField monto = formulario.texto("Monto del abono (S/)", "");
            if (formulario.aceptar(this, "Registrar abono · " + titulo)) {
                controlador.pagar(membresia.getId(), monto.getText(), Formulario.elegido(metodo), false);
            }
        }
    }

    // Historial de pagos
    private void mostrarHistorial() {
        Membresia actual = saldos.seleccion();
        List<Pago> filas = new ArrayList<>();
        if (actual != null) {
            for (Pago pago : estado.getPagos()) {
                if (pago.getMembresiaId() == actual.getId()) filas.add(pago);
            }
        }
        historial.mostrar(filas, elemento -> elemento.getId());
    }

    // Observer: actualización de pagos
    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        saldos.mostrar(estado.getMembresias(), elemento -> elemento.getId());
        mostrarHistorial();
    }
}
