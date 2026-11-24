package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JSplitPane;
import modelo.EstadoGym;
import modelo.Membresia;
import modelo.Pago;
import modelo.Validador;

final class PanelPagos extends PanelModulo {
    private final GymControlador controlador;
    private final Tabla<Membresia> saldos = new Tabla<>(Tabla.numero("Membresía", Membresia::getId),
            Tabla.texto("Socio", Membresia::getSocio), Tabla.texto("Plan", Membresia::getPlan),
            Tabla.texto("Importe", m -> Validador.soles(m.getPrecio())),
            Tabla.texto("Total pagado", m -> Validador.soles(m.getPagado())),
            Tabla.texto("Saldo", m -> Validador.soles(m.saldo())), Tabla.texto("Estado", Membresia::estadoPago));
    private final Tabla<Pago> historial = new Tabla<>(Tabla.numero("ID", Pago::getId),
            Tabla.texto("Fecha y hora", p -> p.getFecha().withNano(0).toString().replace('T', ' ')),
            Tabla.texto("Monto", p -> Validador.soles(p.getMonto())), Tabla.texto("Método", Pago::getMetodo));

    PanelPagos(GymControlador controlador) {
        super("Pagos", "Registra abonos y consulta el historial. El acceso exige un mínimo pagado del 50 %.");
        this.controlador = controlador;
        accion("Registrar abono", true, () -> pagar(false));
        accion("Pagar saldo completo", false, () -> pagar(true));
        var resumen = seccion("Saldos por membresía", saldos);
        resumen.setPreferredSize(new Dimension(500, 220));
        JSplitPane dividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resumen,
                seccion("Abonos de la membresía seleccionada", historial));
        dividido.setBorder(null);
        dividido.setResizeWeight(0.58);
        saldos.alSeleccionar(this::mostrarHistorial);
        add(dividido, BorderLayout.CENTER);
    }

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

    private void mostrarHistorial() {
        Membresia actual = saldos.seleccion();
        List<Pago> filas = new ArrayList<>();
        if (actual != null) {
            for (Pago pago : estado.getPagos()) {
                if (pago.getMembresiaId() == actual.getId()) filas.add(pago);
            }
        }
        historial.mostrar(filas, Pago::getId);
    }

    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        saldos.mostrar(estado.getMembresias(), Membresia::getId);
        mostrarHistorial();
    }
}
