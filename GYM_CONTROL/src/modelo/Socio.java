package modelo;

import java.time.LocalDate;

public final class Socio {
    public enum Estado { ACTIVO, SUSPENDIDO }

    // Atributos
    private final int id;
    private final String dni;
    private final String nombres;
    private final String apellidos;
    private final String telefono;
    private final LocalDate fechaRegistro;
    private final Estado estado;

    // Constructor
    public Socio(int id, String dni, String nombres, String apellidos,
            String telefono, LocalDate fechaRegistro, Estado estado) {
        this.id = id;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public Estado getEstado() {
        return estado;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    @Override
    public String toString() {
        return nombreCompleto() + " · " + dni;
    }

    public boolean mismosDatos(Socio otro) {
        return otro != null && id == otro.id && dni.equals(otro.dni)
                && nombres.equals(otro.nombres) && apellidos.equals(otro.apellidos)
                && telefono.equals(otro.telefono) && fechaRegistro.equals(otro.fechaRegistro)
                && estado == otro.estado;
    }

    public Socio conEstado(Estado nuevoEstado) {
        return new Socio(id, dni, nombres, apellidos, telefono, fechaRegistro, nuevoEstado);
    }
}
