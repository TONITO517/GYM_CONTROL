package modelo.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class Consultas {
    @FunctionalInterface
    interface Fila<T> {
        T leer(ResultSet datos) throws SQLException;
    }

    private Consultas() {}

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

    static int contar(Connection conexion, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = preparar(conexion, sql, parametros);
             ResultSet datos = sentencia.executeQuery()) {
            return datos.next() ? datos.getInt(1) : 0;
        }
    }

    static int actualizar(Connection conexion, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = preparar(conexion, sql, parametros)) {
            return sentencia.executeUpdate();
        }
    }

    static int insertar(Connection conexion, String sql, Object... parametros) throws SQLException {
        actualizar(conexion, sql, parametros);
        return contar(conexion, "SELECT last_insert_rowid()");
    }
}
