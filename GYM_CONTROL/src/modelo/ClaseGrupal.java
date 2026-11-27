package modelo;

import java.time.LocalDateTime;

public final class ClaseGrupal {
    // Atributos
    private final int id;
    private final String nombre;
    private final String entrenador;
    private final String sede;
    private final LocalDateTime horario;
    private final int capacidad;
    private final int inscritos;

    // Constructor
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

    // Getters
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

    // Datos relacionados con clasegrupal
    public static final class ReservaClase {
        private final int claseId;
        private final int socioId;
        private final String socio;
        private final LocalDateTime fecha;

        public ReservaClase(int claseId, int socioId, String socio, LocalDateTime fecha) {
            this.claseId = claseId;
            this.socioId = socioId;
            this.socio = socio;
            this.fecha = fecha;
        }

        public int getClaseId() {
            return claseId;
        }

        public int getSocioId() {
            return socioId;
        }

        public String getSocio() {
            return socio;
        }

        public LocalDateTime getFecha() {
            return fecha;
        }


        @Override
        public String toString() {
            return socio;
        }
    }

    // Datos relacionados con clasegrupal
    public static final class Entrenador {
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
}
