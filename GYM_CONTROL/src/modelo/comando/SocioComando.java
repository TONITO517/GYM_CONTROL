package modelo.comando;

import modelo.GymModelo;
import modelo.Socio;

public final class SocioComando implements Comando {
    // Datos anteriores y nuevos del socio
    private final GymModelo modelo;
    private final Socio anterior;
    private final String dni, nombres, apellidos, telefono;
    private final String descripcion;
    private Socio nuevo;

    // Registro de un nuevo socio.
    public SocioComando(GymModelo modelo, String dni, String nombres, String apellidos, String telefono) {
        this.modelo = modelo;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        anterior = null;
        descripcion = "registro de socio";
    }

    // Edición de los datos del socio.
    public SocioComando(GymModelo modelo, Socio anterior, Socio nuevo) {
        this(modelo, anterior, nuevo, "edición de socio");
    }

    // Suspensión o activación de un socio.
    public SocioComando(GymModelo modelo, Socio anterior, Socio.Estado estado) {
        this(modelo, anterior, anterior.conEstado(estado), "cambio de estado de socio");
    }

    private SocioComando(GymModelo modelo, Socio anterior, Socio nuevo, String descripcion) {
        this.modelo = modelo;
        this.anterior = anterior;
        this.nuevo = nuevo;
        this.descripcion = descripcion;
        dni = nombres = apellidos = telefono = null;
    }

    // Ejecución y restauración
    @Override
    public void ejecutar() {
        if (anterior != null) {
            nuevo = modelo.reemplazarSocio(anterior, nuevo);
        } else if (nuevo == null) {
            nuevo = modelo.registrarSocio(dni, nombres, apellidos, telefono);
        } else {
            nuevo = modelo.insertarSocio(nuevo);
        }
    }

    // Deshacer el cambio
    @Override
    public void deshacer() {
        if (anterior == null) {
            modelo.eliminarRegistroSocio(nuevo);
        } else {
            modelo.reemplazarSocio(nuevo, anterior);
        }
    }

    @Override
    public String descripcion() { return descripcion; }
}
