package modelo.estrategia;

import modelo.Validador;

public final class DescuentoNuevoSocio implements EstrategiaPrecio {
    // Porcentaje de descuento
    private static final int DESCUENTO = 10;

    @Override
    public String nombre() {
        return "Nuevo socio (" + DESCUENTO + " %)";
    }

    // Cálculo y validación del primer contrato
    @Override
    public int calcular(Contexto contexto) {
        Validador.exigir(contexto.esPrimeraMembresia(),
                "El descuento solo aplica al primer contrato del socio.");
        // Se suman 50 unidades antes de dividir para redondear al céntimo.
        long importe = (100L - DESCUENTO) * contexto.getPrecioBase();
        return (int) ((importe + 50) / 100);
    }

    @Override public String toString() { return nombre(); }
}
