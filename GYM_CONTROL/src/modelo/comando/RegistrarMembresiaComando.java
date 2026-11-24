package modelo.comando;

import java.util.function.IntSupplier;
import modelo.EstadoGym;
import modelo.GymModelo;
import modelo.Membresia;

public final class RegistrarMembresiaComando implements Comando {
    private final GymModelo modelo;
    private final IntSupplier registrar;
    private Membresia registrada;

    public RegistrarMembresiaComando(GymModelo modelo, IntSupplier registrar) {
        this.modelo = modelo;
        this.registrar = registrar;
    }

    @Override
    public void ejecutar() {
        if (registrada == null) {
            int id = registrar.getAsInt();
            EstadoGym estado = modelo.consultar();
            registrada = estado.getMembresias().stream()
                    .filter(membresia -> membresia.getId() == id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No se encontró la membresía recién creada."));
        } else {
            modelo.restaurarMembresia(registrada);
        }
    }

    @Override
    public void deshacer() {
        modelo.eliminarMembresia(registrada.getId());
    }

    @Override
    public String descripcion() {
        return "registro de membresía";
    }
}
