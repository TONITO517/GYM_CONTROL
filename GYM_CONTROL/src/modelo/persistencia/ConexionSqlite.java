package modelo.persistencia;

import excepciones.PersistenciaException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class ConexionSqlite {
    private final String url;

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    public ConexionSqlite(Path archivo) {
        url = "jdbc:sqlite:" + archivo.toAbsolutePath();
    }

    public void inicializar(boolean demostracion) {
        transaccion(conexion -> {
            ejecutarRecurso(conexion, "/esquema.sql");
            if (demostracion && Consultas.contar(conexion, "PRAGMA user_version") == 0) {
                cargarDemostracion(conexion);
            }
            return null;
        });
    }

    private void cargarDemostracion(Connection conexion) throws SQLException {
        boolean baseVacia = Consultas.contar(conexion, """
                SELECT EXISTS(SELECT 1 FROM socio) OR EXISTS(SELECT 1 FROM sede)
                    OR EXISTS(SELECT 1 FROM entrenador)
                """) == 0;
        if (baseVacia) ejecutarRecurso(conexion, "/demostracion.sql");
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.execute("PRAGMA user_version = 1");
        }
    }

    private void ejecutarRecurso(Connection conexion, String ruta) throws SQLException {
        try (InputStream recurso = getClass().getResourceAsStream(ruta);
             Statement sentencia = conexion.createStatement()) {
            if (recurso == null) throw new IOException("No se encontró " + ruta + ".");
            String archivo = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
            for (String sql : archivo.split(";")) {
                if (!sql.isBlank()) sentencia.execute(sql);
            }
        } catch (IOException error) {
            throw new PersistenciaException("No se pudo leer " + ruta + ".", error);
        }
    }

    public <T> T transaccion(Operacion<T> operacion) {
        Properties opciones = new Properties();
        opciones.setProperty("foreign_keys", "true");
        opciones.setProperty("busy_timeout", "4000");
        // Reserva la escritura antes de consultar cupos, saldos y estados.
        opciones.setProperty("transaction_mode", "IMMEDIATE");
        try (Connection conexion = DriverManager.getConnection(url, opciones)) {
            conexion.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException error) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    error.addSuppressed(errorRollback);
                }
                throw error;
            }
        } catch (SQLException error) {
            throw new PersistenciaException("La operación en SQLite no se completó. " + error.getMessage(), error);
        }
    }
}
