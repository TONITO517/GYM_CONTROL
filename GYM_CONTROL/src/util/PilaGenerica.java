package util;

import java.util.ArrayList;

public final class PilaGenerica<T> {
    private final ArrayList<T> elementos = new ArrayList<>();

    public void apilar(T elemento) {
        elementos.add(elemento);
    }

    public T cima() {
        int ultimo = elementos.size() - 1;
        return elementos.get(ultimo);
    }

    public T desapilar() {
        int ultimo = elementos.size() - 1;
        return elementos.remove(ultimo);
    }

    public boolean vacia() { return elementos.isEmpty(); }
    public void limpiar() { elementos.clear(); }
}
