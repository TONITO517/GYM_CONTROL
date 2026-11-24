package modelo;

import java.time.LocalDateTime;

public final class Asistencia {
    private final int id;
    private final int socioId;
    private final int membresiaId;
    private final int sedeId;
    private final String socio;
    private final String sede;
    private final LocalDateTime entrada;
    private final LocalDateTime salida;

    public Asistencia(int id, int socioId, int membresiaId, int sedeId,
            String socio, String sede, LocalDateTime entrada, LocalDateTime salida) {
        this.id = id;
        this.socioId = socioId;
        this.membresiaId = membresiaId;
        this.sedeId = sedeId;
        this.socio = socio;
        this.sede = sede;
        this.entrada = entrada;
        this.salida = salida;
    }

    public int getId() {
        return id;
    }

    public int getSocioId() {
        return socioId;
    }

    public int getMembresiaId() {
        return membresiaId;
    }

    public int getSedeId() {
        return sedeId;
    }

    public String getSocio() {
        return socio;
    }

    public String getSede() {
        return sede;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public LocalDateTime getSalida() {
        return salida;
    }
}
