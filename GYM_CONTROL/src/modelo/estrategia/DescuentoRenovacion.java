package modelo.estrategia;

import modelo.Membresia;
import modelo.Validador;

public final class DescuentoRenovacion implements EstrategiaPrecio {
    // Porcentaje de descuento
    private static final int DESCUENTO = 5;

    @Override
    public String nombre() {
        return "Renovación anticipada (" + DESCUENTO + " %)";
    }

    // Cálculo y validación de la renovación
    @Override
    public int calcular(Contexto contexto) {
        Membresia anterior = contexto.getAnterior();
        Validador.exigir(anterior != null && anterior.vigencia(contexto.getHoy()) == Membresia.Vigencia.POR_VENCER,
                "Este descuento aplica a la renovación de una membresía vigente en sus últimos 7 días.");
        // Se suman 50 unidades antes de dividir para redondear al céntimo.
        long importe = (100L - DESCUENTO) * contexto.getPrecioBase();
        return (int) ((importe + 50) / 100);
    }

    @Override public String toString() { return nombre(); }
}
