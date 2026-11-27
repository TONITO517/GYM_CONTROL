package modelo.comando;

// Command: interfaz de las operaciones
public interface Comando {
    void ejecutar();
    void deshacer();
    String descripcion();
}
