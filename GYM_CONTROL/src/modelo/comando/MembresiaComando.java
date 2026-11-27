package modelo.comando;

import modelo.GymModelo;
import modelo.Membresia;
import modelo.Validador;
import modelo.estrategia.EstrategiaPrecio;

public final class MembresiaComando implements Comando {
    // Datos del comando
    private final GymModelo modelo;
    private final int socio, plan, anterior;
    private final String inicio;
    private final boolean renovacion;
    private final EstrategiaPrecio estrategia;
    private Membresia registrada;

    // Constructor para contratación
    public MembresiaComando(GymModelo modelo, int socio, int plan,
            String inicio, EstrategiaPrecio estrategia) {
        this.modelo = modelo;
        this.socio = socio;
        this.plan = plan;
        this.inicio = inicio;
        this.estrategia = estrategia;
        anterior = 0;
        renovacion = false;
    }

    // Constructor para renovación
    public MembresiaComando(GymModelo modelo, int socio, int plan,
            int anterior, EstrategiaPrecio estrategia) {
        this.modelo = modelo;
        this.socio = socio;
        this.plan = plan;
        this.anterior = anterior;
        this.estrategia = estrategia;
        inicio = null;
        renovacion = true;
    }

    // Ejecutar o restaurar la membresía
    @Override
    public void ejecutar() {
        if (registrada != null) {
            modelo.restaurarMembresia(registrada);
            return;
        }
        int id;
        if (renovacion) {
            id = modelo.renovar(socio, plan, anterior, estrategia);
        } else {
            id = modelo.contratar(socio, plan, Validador.fecha(inicio), estrategia);
        }
        for (Membresia membresia : modelo.consultar().getMembresias()) {
            if (membresia.getId() == id) {
                registrada = membresia;
                return;
            }
        }
        throw new IllegalStateException("No se encontró la membresía recién creada.");
    }

    // Deshacer la contratación
    @Override
    public void deshacer() {
        modelo.eliminarMembresia(registrada.getId());
    }

    @Override
    public String descripcion() { return "registro de membresía"; }
}
