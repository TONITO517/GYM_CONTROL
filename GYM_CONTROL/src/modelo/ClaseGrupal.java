package modelo;

import java.time.LocalDateTime;

public final class ClaseGrupal {
    private final int id;
    private final String nombre;
    private final String entrenador;
    private final String sede;
    private final LocalDateTime horario;
    private final int capacidad;
    private final int inscritos;

    public ClaseGrupal(int id, String nombre, String entrenador, String sede,
            LocalDateTime horario, int capacidad, int inscritos) {
        this.id = id;
        this.nombre = nombre;
        this.entrenador = entrenador;
        this.sede = sede;
        this.horario = horario;
        this.capacidad = capacidad;
        this.inscritos = inscritos;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEntrenador() {
        return entrenador;
    }

    public String getSede() {
        return sede;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getInscritos() {
        return inscritos;
    }


    public int disponibles() {
        return capacidad - inscritos;
    }
}
