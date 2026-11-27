package util;

import java.util.ArrayList;

public final class PilaGenerica<T> {
    // Lista genérica de elementos
    private final ArrayList<T> elementos = new ArrayList<>();

    // Apilar un elemento
    public void apilar(T elemento) {
        elementos.add(elemento);
    }

    // Consultar la cima
    public T cima() {
        if (vacia()) {
            throw new IllegalStateException("La pila está vacía.");
        }
        int ultimo = elementos.size() - 1;
        return elementos.get(ultimo);
    }

    // Desapilar un elemento
    public T desapilar() {
        if (vacia()) {
            throw new IllegalStateException("La pila está vacía.");
        }
        int ultimo = elementos.size() - 1;
        return elementos.remove(ultimo);
    }

    // Estado y limpieza de la pila
    public boolean vacia() { return elementos.isEmpty(); }
    public void limpiar() { elementos.clear(); }
}
