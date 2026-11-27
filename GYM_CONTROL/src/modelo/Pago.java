package modelo;

import java.time.LocalDateTime;

public final class Pago {
    public enum Metodo { EFECTIVO, TARJETA, YAPE, PLIN, TRANSFERENCIA }

    // Atributos
    private final int id;
    private final int membresiaId;
    private final LocalDateTime fecha;
    private final int monto;
    private final Metodo metodo;

    // Constructor
    public Pago(int id, int membresiaId, LocalDateTime fecha, int monto, Metodo metodo) {
        this.id = id;
        this.membresiaId = membresiaId;
        this.fecha = fecha;
        this.monto = monto;
        this.metodo = metodo;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getMembresiaId() {
        return membresiaId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public int getMonto() {
        return monto;
    }

    public Metodo getMetodo() {
        return metodo;
    }


}
