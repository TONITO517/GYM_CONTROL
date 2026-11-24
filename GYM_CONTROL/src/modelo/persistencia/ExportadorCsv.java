package modelo.persistencia;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.IntConsumer;
import modelo.Asistencia;

public final class ExportadorCsv {
    private ExportadorCsv() {}

    public static void asistencias(Path destino, List<Asistencia> filas, IntConsumer progreso) throws IOException {
        Path archivo = destino.toAbsolutePath();
        Path temporal = Files.createTempFile(archivo.getParent(), "gym-reporte-", ".tmp");
        try {
            try (BufferedWriter salida = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                salida.write("ID,Socio,Sede,Entrada,Salida");
                salida.newLine();
                int escritas = 0;
                for (Asistencia fila : filas) {
                    String fin = fila.getSalida() == null ? "" : fila.getSalida().toString();
                    salida.write(fila.getId() + "," + celda(fila.getSocio()) + "," + celda(fila.getSede())
                            + "," + celda(fila.getEntrada().toString()) + "," + celda(fin));
                    salida.newLine();
                    progreso.accept((int) (++escritas * 95L / filas.size()));
                }
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
            progreso.accept(100);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static String celda(String texto) {
        String limpio = texto.stripLeading();
        if (!limpio.isEmpty() && "=+-@".indexOf(limpio.charAt(0)) >= 0) texto = "'" + texto;
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
