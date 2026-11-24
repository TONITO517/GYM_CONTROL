package modelo.comando;

import java.util.function.IntSupplier;
import modelo.EstadoGym;
import modelo.GymModelo;
import modelo.Pago;

public final class RegistrarPagoComando implements Comando {
    private final GymModelo modelo;
    private final IntSupplier registrar;
    private Pago registrado;

    public RegistrarPagoComando(GymModelo modelo, IntSupplier registrar) {
        this.modelo = modelo;
        this.registrar = registrar;
    }

    @Override
    public void ejecutar() {
        if (registrado == null) {
            int id = registrar.getAsInt();
            EstadoGym estado = modelo.consultar();
            registrado = estado.getPagos().stream()
                    .filter(pago -> pago.getId() == id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No se encontró el abono recién registrado."));
        } else {
            modelo.restaurarPago(registrado);
        }
    }

    @Override
    public void deshacer() {
        modelo.eliminarPago(registrado);
    }

    @Override
    public String descripcion() {
        return "registro de abono";
    }
}
