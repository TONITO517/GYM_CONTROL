package modelo;

public final class Sede {
    // Atributos
    private final int id;
    private final String nombre;
    private final String direccion;
    private final int capacidad;
    private final int presentes;

    // Constructor
    public Sede(int id, String nombre, String direccion, int capacidad, int presentes) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.capacidad = capacidad;
        this.presentes = presentes;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getPresentes() {
        return presentes;
    }


    public int disponibles() {
        return capacidad - presentes;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
