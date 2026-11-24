package modelo.estrategia;

public final class PrecioNormal implements EstrategiaPrecio {
    @Override public String nombre() { return "Tarifa normal"; }

    @Override
    public int calcular(Contexto contexto) {
        return contexto.getPrecioBase();
    }

    @Override public String toString() { return nombre(); }
}
