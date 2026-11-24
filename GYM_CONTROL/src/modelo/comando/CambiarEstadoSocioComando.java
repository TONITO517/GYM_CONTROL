package modelo.comando;

import modelo.GymModelo;
import modelo.Socio;

public final class CambiarEstadoSocioComando implements Comando {
    private final GymModelo modelo;
    private final Socio anterior;
    private final Socio siguiente;

    public CambiarEstadoSocioComando(GymModelo modelo, Socio anterior, Socio.Estado estado) {
        this.modelo = modelo;
        this.anterior = anterior;
        siguiente = anterior.conEstado(estado);
    }

    @Override public void ejecutar() { modelo.reemplazarSocio(anterior, siguiente); }
    @Override public void deshacer() { modelo.reemplazarSocio(siguiente, anterior); }
    @Override public String descripcion() { return "cambio de estado de socio"; }
}
