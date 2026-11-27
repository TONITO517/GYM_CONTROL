package modelo;

import java.util.List;

public final class Plan {
    // Atributos
    private final int id;
    private final String nombre;
    private final int meses;
    private final int precio;
    private final List<Integer> sedes;

    // Constructor
    public Plan(int id, String nombre, int meses, int precio, List<Integer> sedes) {
        this.id = id;
        this.nombre = nombre;
        this.meses = meses;
        this.precio = precio;
        this.sedes = List.copyOf(sedes);
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getMeses() {
        return meses;
    }

    public int getPrecio() {
        return precio;
    }

    public List<Integer> getSedes() {
        return sedes;
    }


    @Override
    public String toString() {
        return nombre + " · " + Validador.soles(precio);
    }
}
