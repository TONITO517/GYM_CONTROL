package excepciones;

public class DatosInvalidosException extends RuntimeException {
    // Constructor de la excepción
    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
