package modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EstadoGym {
    private final LocalDateTime momento;
    private final List<Socio> socios;
    private final List<Sede> sedes;
    private final List<Plan> planes;
    private final List<Membresia> membresias;
    private final List<Pago> pagos;
    private final List<Asistencia> asistencias;
    private final List<Entrenador> entrenadores;
    private final List<ClaseGrupal> clases;
    private final List<ReservaClase> reservas;

    public EstadoGym(LocalDateTime momento, List<Socio> socios, List<Sede> sedes, List<Plan> planes,
            List<Membresia> membresias, List<Pago> pagos, List<Asistencia> asistencias, List<Entrenador> entrenadores,
            List<ClaseGrupal> clases, List<ReservaClase> reservas) {
        this.momento = momento;
        this.socios = List.copyOf(socios);
        this.sedes = List.copyOf(sedes);
        this.planes = List.copyOf(planes);
        this.membresias = List.copyOf(membresias);
        this.pagos = List.copyOf(pagos);
        this.asistencias = List.copyOf(asistencias);
        this.entrenadores = List.copyOf(entrenadores);
        this.clases = List.copyOf(clases);
        this.reservas = List.copyOf(reservas);
    }

    public LocalDateTime getMomento() {
        return momento;
    }

    public List<Socio> getSocios() {
        return socios;
    }

    public List<Sede> getSedes() {
        return sedes;
    }

    public List<Plan> getPlanes() {
        return planes;
    }

    public List<Membresia> getMembresias() {
        return membresias;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public List<Asistencia> getAsistencias() {
        return asistencias;
    }

    public List<Entrenador> getEntrenadores() {
        return entrenadores;
    }

    public List<ClaseGrupal> getClases() {
        return clases;
    }

    public List<ReservaClase> getReservas() {
        return reservas;
    }


    public LocalDate hoy() {
        return momento.toLocalDate();
    }

    public long activos() {
        int cantidad = 0;
        for (Socio socio : socios) {
            if (socio.getEstado() == Socio.Estado.ACTIVO) cantidad++;
        }
        return cantidad;
    }

    public long porVigencia(Membresia.Vigencia estado) {
        int cantidad = 0;
        for (Membresia membresia : membresias) {
            if (membresia.vigencia(hoy()) == estado) cantidad++;
        }
        return cantidad;
    }

    public long pagosPendientes() {
        int cantidad = 0;
        for (Membresia membresia : membresias) {
            if (membresia.saldo() > 0) cantidad++;
        }
        return cantidad;
    }

    public int presentes() {
        int cantidad = 0;
        for (Sede sede : sedes) {
            cantidad += sede.getPresentes();
        }
        return cantidad;
    }

    public long clasesProgramadas() {
        int cantidad = 0;
        for (ClaseGrupal clase : clases) {
            if (clase.getHorario().isAfter(momento)) cantidad++;
        }
        return cantidad;
    }
}
