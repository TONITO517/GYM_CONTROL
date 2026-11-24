package modelo.comando;

import modelo.GymModelo;
import modelo.Socio;

public final class EditarSocioComando implements Comando {
    private final GymModelo modelo;
    private final Socio anterior;
    private Socio nuevo;

    public EditarSocioComando(GymModelo modelo, Socio anterior, Socio nuevo) {
        this.modelo = modelo;
        this.anterior = anterior;
        this.nuevo = nuevo;
    }

    @Override public void ejecutar() { nuevo = modelo.reemplazarSocio(anterior, nuevo); }
    @Override public void deshacer() { modelo.reemplazarSocio(nuevo, anterior); }
    @Override public String descripcion() { return "edición de socio"; }
}
