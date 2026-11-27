package modelo.comando;

import modelo.GymModelo;
import modelo.Pago;
import modelo.Validador;

public final class PagoComando implements Comando {
    // Datos del pago
    private final GymModelo modelo;
    private final int membresia;
    private final String monto;
    private final Pago.Metodo metodo;
    private final boolean cancelarSaldo;
    private Pago registrado;

    // Constructor
    public PagoComando(GymModelo modelo, int membresia, String monto,
            Pago.Metodo metodo, boolean cancelarSaldo) {
        this.modelo = modelo;
        this.membresia = membresia;
        this.monto = monto;
        this.metodo = metodo;
        this.cancelarSaldo = cancelarSaldo;
    }

    // Ejecutar o restaurar el pago
    @Override
    public void ejecutar() {
        if (registrado != null) {
            modelo.restaurarPago(registrado);
            return;
        }
        int id;
        if (cancelarSaldo) {
            id = modelo.pagarSaldo(membresia, metodo);
        } else {
            id = modelo.abonar(membresia, Validador.centimos(monto), metodo);
        }
        for (Pago pago : modelo.consultar().getPagos()) {
            if (pago.getId() == id) {
                registrado = pago;
                return;
            }
        }
        throw new IllegalStateException("No se encontró el abono recién registrado.");
    }

    // Deshacer el pago
    @Override
    public void deshacer() { modelo.eliminarPago(registrado); }

    @Override
    public String descripcion() { return "registro de abono"; }
}
