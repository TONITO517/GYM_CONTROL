package modelo.comando;

import modelo.GymModelo;
import modelo.Socio;

public final class RegistrarSocioComando implements Comando {
    private final GymModelo modelo;
    private final String dni;
    private final String nombres;
    private final String apellidos;
    private final String telefono;
    private Socio registrado;

    public RegistrarSocioComando(GymModelo modelo, String dni, String nombres, String apellidos, String telefono) {
        this.modelo = modelo;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
    }

    @Override
    public void ejecutar() {
        if (registrado == null) {
            registrado = modelo.registrarSocio(dni, nombres,
                    apellidos, telefono);
        } else {
            // Rehace el alta con el ID original.
            registrado = modelo.insertarSocio(registrado);
        }
    }

    @Override public void deshacer() { modelo.eliminarRegistroSocio(registrado); }
    @Override public String descripcion() { return "registro de socio"; }
}
