package modelo.persistencia;

import excepciones.PersistenciaException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ConexionSqlite {
    // Ruta de la base de datos
    private final String url;

    // Interfaz para las transacciones
    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    // Conexión con SQLite
    public ConexionSqlite(Path archivo) {
        url = "jdbc:sqlite:" + archivo.toAbsolutePath();
    }

    // Creación de tablas y datos iniciales
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

    // Lectura de archivos SQL
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

    // Transacciones JDBC
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
                // Commit
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException error) {
                try {
                    // Rollback
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

    // Métodos JDBC reutilizados por los DAO.
    static final class Consultas {
        @FunctionalInterface
        interface Fila<T> {
            T leer(ResultSet datos) throws SQLException;
        }

        private Consultas() {}

        // PreparedStatement y parámetros SQL
        private static PreparedStatement preparar(Connection conexion, String sql, Object... parametros)
                throws SQLException {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            try {
                for (int i = 0; i < parametros.length; i++) {
                    sentencia.setObject(i + 1, parametros[i]);
                }
                return sentencia;
            } catch (SQLException error) {
                sentencia.close();
                throw error;
            }
        }

        // Consulta de varios registros
        static <T> List<T> listar(Connection conexion, String sql, Fila<T> fila, Object... parametros)
                throws SQLException {
            List<T> resultados = new ArrayList<>();
            try (PreparedStatement sentencia = preparar(conexion, sql, parametros);
                 ResultSet datos = sentencia.executeQuery()) {
                while (datos.next()) {
                    resultados.add(fila.leer(datos));
                }
            }
            return List.copyOf(resultados);
        }

        // Consulta de un registro
        static <T> T uno(Connection conexion, String sql, Fila<T> fila, Object... parametros)
                throws SQLException {
            try (PreparedStatement sentencia = preparar(conexion, sql, parametros);
                 ResultSet datos = sentencia.executeQuery()) {
                if (datos.next()) {
                    return fila.leer(datos);
                }
                return null;
            }
        }

        // Consulta de cantidades
        static int contar(Connection conexion, String sql, Object... parametros) throws SQLException {
            try (PreparedStatement sentencia = preparar(conexion, sql, parametros);
                 ResultSet datos = sentencia.executeQuery()) {
                return datos.next() ? datos.getInt(1) : 0;
            }
        }

        // Actualización de registros
        static int actualizar(Connection conexion, String sql, Object... parametros) throws SQLException {
            try (PreparedStatement sentencia = preparar(conexion, sql, parametros)) {
                return sentencia.executeUpdate();
            }
        }

        // Inserción y recuperación del ID
        static int insertar(Connection conexion, String sql, Object... parametros) throws SQLException {
            actualizar(conexion, sql, parametros);
            return contar(conexion, "SELECT last_insert_rowid()");
        }
    }
}
