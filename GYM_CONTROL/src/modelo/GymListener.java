package modelo;

// Observer: interfaz de notificación
@FunctionalInterface
public interface GymListener {
    void gymActualizado(EstadoGym estado);
}
