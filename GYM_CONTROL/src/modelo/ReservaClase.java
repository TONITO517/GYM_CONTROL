package modelo;

import java.time.LocalDateTime;

public final class ReservaClase {
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
