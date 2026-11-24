package modelo;

import excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class Validador {
    private Validador() {}

    public static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new DatosInvalidosException(mensaje);
        }
    }

    public static String texto(String valor, String campo) {
        String limpio = valor == null ? "" : valor.strip();
        exigir(!limpio.isEmpty(), "Completa el campo " + campo + ".");
        return limpio;
    }

    public static String dni(String valor) {
        String limpio = texto(valor, "DNI");
        exigir(limpio.matches("[0-9]{8}"), "El DNI debe tener 8 dígitos.");
        return limpio;
    }

    public static int entero(String valor, String campo) {
        try {
            return Integer.parseInt(texto(valor, campo));
        } catch (NumberFormatException error) {
            throw new DatosInvalidosException(campo + " debe ser un número entero.");
        }
    }

    public static int centimos(String valor) {
        try {
            return new BigDecimal(texto(valor, "importe").replace(',', '.'))
                    .movePointRight(2).intValueExact();
        } catch (NumberFormatException | ArithmeticException error) {
            throw new DatosInvalidosException("Ingresa un importe válido con hasta dos decimales.");
        }
    }

    public static String soles(int centimos) {
        return String.format(Locale.ROOT, "S/ %.2f", BigDecimal.valueOf(centimos, 2));
    }

    public static LocalDate fecha(String valor) {
        try {
            return LocalDate.parse(texto(valor, "fecha"));
        } catch (DateTimeParseException error) {
            throw new DatosInvalidosException("Usa una fecha válida en formato AAAA-MM-DD.");
        }
    }

    public static LocalDateTime horario(String valor) {
        try {
            return LocalDateTime.parse(texto(valor, "fecha y hora").replace(' ', 'T'));
        } catch (DateTimeParseException error) {
            throw new DatosInvalidosException("Usa AAAA-MM-DD HH:mm para la fecha y hora.");
        }
    }
}
