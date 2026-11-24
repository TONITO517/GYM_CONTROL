package vista;

import controlador.GymControlador;
import java.awt.BorderLayout;
import javax.swing.JTextField;
import modelo.EstadoGym;
import modelo.Sede;

final class PanelSedes extends PanelModulo {
    private final GymControlador controlador;
    private final Tabla<Sede> tabla = new Tabla<>(Tabla.numero("ID", Sede::getId), Tabla.texto("Sede", Sede::getNombre),
            Tabla.texto("Dirección", Sede::getDireccion), Tabla.numero("Capacidad", Sede::getCapacidad),
            Tabla.numero("Personas dentro", Sede::getPresentes), Tabla.numero("Espacios libres", Sede::disponibles));

    PanelSedes(GymControlador controlador) {
        super("Sedes", "Administra direcciones y capacidad. El aforo se actualiza con cada entrada y salida.");
        this.controlador = controlador;
        accion("Nueva sede", true, () -> formulario(null));
        accion("Editar sede", false, () -> {
            Sede sede = seleccion(tabla, "una sede");
            if (sede != null) formulario(sede);
        });
        add(tabla, BorderLayout.CENTER);
    }

    private void formulario(Sede sede) {
        Formulario formulario = new Formulario();
        JTextField nombre = formulario.texto("Nombre", sede == null ? "" : sede.getNombre());
        JTextField direccion = formulario.texto("Dirección", sede == null ? "" : sede.getDireccion());
        JTextField capacidad = formulario.texto("Capacidad máxima", sede == null ? "" : Integer.toString(sede.getCapacidad()));
        if (formulario.aceptar(this, sede == null ? "Registrar sede" : "Editar sede")) {
            controlador.sede(sede == null ? 0 : sede.getId(), nombre.getText(), direccion.getText(), capacidad.getText());
        }
    }

    @Override
    public void gymActualizado(EstadoGym estado) {
        this.estado = estado;
        tabla.mostrar(estado.getSedes(), Sede::getId);
    }
}
