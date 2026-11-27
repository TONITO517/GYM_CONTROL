package modelo;

import java.time.LocalDate;

public final class Membresia {
    // Estados de vigencia
    public enum Vigencia {
        PROGRAMADA, VIGENTE, POR_VENCER, VENCIDA, CONGELADA
    }

    // Atributos
    private final int id;
    private final int socioId;
    private final int planId;
    private final String socio;
    private final String plan;
    private final LocalDate inicio;
    private final LocalDate vencimiento;
    private final int precio;
    private final int pagado;
    private final String tarifa;
    private final Congelamiento congelamiento;

    // Constructor
    public Membresia(int id, int socioId, int planId, String socio,
            String plan, LocalDate inicio, LocalDate vencimiento, int precio,
            int pagado, String tarifa, Congelamiento congelamiento) {
        this.id = id;
        this.socioId = socioId;
        this.planId = planId;
        this.socio = socio;
        this.plan = plan;
        this.inicio = inicio;
        this.vencimiento = vencimiento;
        this.precio = precio;
        this.pagado = pagado;
        this.tarifa = tarifa;
        this.congelamiento = congelamiento;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getSocioId() {
        return socioId;
    }

    public int getPlanId() {
        return planId;
    }

    public String getSocio() {
        return socio;
    }

    public String getPlan() {
        return plan;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getVencimiento() {
        return vencimiento;
    }

    public int getPrecio() {
        return precio;
    }

    public int getPagado() {
        return pagado;
    }

    public String getTarifa() {
        return tarifa;
    }

    public Congelamiento getCongelamiento() {
        return congelamiento;
    }



    // Saldo y estado del pago
    public int saldo() {
        return precio - pagado;
    }

    public String estadoPago() {
        if (saldo() == 0) return "Pagado";
        if (pagado > 0) return "Parcial";
        return "Pendiente";
    }

    // Abono mínimo
    public boolean abonoSuficiente() {
        // Redondea hacia arriba al céntimo el mínimo de 50 %.
        return pagado >= precio / 2 + precio % 2;
    }

    // Cálculo de vigencia
    public Vigencia vigencia(LocalDate hoy) {
        if (hoy.isBefore(inicio)) return Vigencia.PROGRAMADA;
        if (hoy.isAfter(vencimiento)) return Vigencia.VENCIDA;
        if (congelamiento != null && congelamiento.activoEn(hoy)) {
            return Vigencia.CONGELADA;
        }
        if (!vencimiento.isAfter(hoy.plusDays(7))) return Vigencia.POR_VENCER;
        return Vigencia.VIGENTE;
    }

    public boolean vigenteEn(LocalDate hoy) {
        Vigencia estado = vigencia(hoy);
        return estado == Vigencia.VIGENTE || estado == Vigencia.POR_VENCER;
    }

    // Texto para mostrar la membresía
    @Override
    public String toString() {
        return "#" + id + " · " + socio + " · " + plan;
    }

    // Datos relacionados con membresia
    public static final class Congelamiento {
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
}
