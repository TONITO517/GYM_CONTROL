package modelo;

import java.time.LocalDate;

public final class Congelamiento {
    private final int id;
    private final int membresiaId;
    private final LocalDate inicio;
    private final LocalDate fin;
    private final String motivo;

    public Congelamiento(int id, int membresiaId, LocalDate inicio, LocalDate fin, String motivo) {
        this.id = id;
        this.membresiaId = membresiaId;
        this.inicio = inicio;
        this.fin = fin;
        this.motivo = motivo;
    }

    public int getId() {
        return id;
    }

    public int getMembresiaId() {
        return membresiaId;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFin() {
        return fin;
    }

    public String getMotivo() {
        return motivo;
    }


    public boolean activoEn(LocalDate dia) {
        return !dia.isBefore(inicio) && dia.isBefore(fin);
    }
}
