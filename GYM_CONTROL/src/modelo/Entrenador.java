package modelo;

public final class Entrenador {
    private final int id;
    private final String nombres;
    private final String apellidos;
    private final String especialidad;
    private final String telefono;
    private final String disponibilidad;

    public Entrenador(int id, String nombres, String apellidos, String especialidad,
            String telefono, String disponibilidad) {
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.disponibilidad = disponibilidad;
    }

    public int getId() {
        return id;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDisponibilidad() {
        return disponibilidad;
    }


    @Override
    public String toString() {
        return nombres + " " + apellidos;
    }
}
