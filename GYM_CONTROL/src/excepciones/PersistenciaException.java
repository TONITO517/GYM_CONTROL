package excepciones;

public class PersistenciaException extends RuntimeException {
    // Constructor de la excepción
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
