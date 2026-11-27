package modelo.estrategia;

// Strategy: tarifa sin descuento
public final class PrecioNormal implements EstrategiaPrecio {
    @Override public String nombre() { return "Tarifa normal"; }

    // Cálculo del precio normal
    @Override
    public int calcular(Contexto contexto) {
        return contexto.getPrecioBase();
    }

    @Override public String toString() { return nombre(); }
}
