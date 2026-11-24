package modelo.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import modelo.Socio;

public final class SocioDAO {
    private static Socio leer(ResultSet datos) throws SQLException {
        return new Socio(datos.getInt("id"), datos.getString("dni"), datos.getString("nombres"),
                datos.getString("apellidos"), datos.getString("telefono"),
                LocalDate.parse(datos.getString("fecha_registro")), Socio.Estado.valueOf(datos.getString("estado")));
    }

    public List<Socio> listar(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, "SELECT * FROM socio ORDER BY apellidos, nombres, id", SocioDAO::leer);
    }

    public Socio porId(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, "SELECT * FROM socio WHERE id = ?", SocioDAO::leer, id);
    }

    public Socio porDni(Connection conexion, String dni)
            throws SQLException {
        String sql = "SELECT * FROM socio WHERE dni = ?";
        try (PreparedStatement consulta =
                conexion.prepareStatement(sql)) {
            consulta.setString(1, dni);
            try (ResultSet filas = consulta.executeQuery()) {
                if (filas.next()) {
                    return leer(filas);
                }
            }
        }
        return null;
    }

    public int insertar(Connection conexion, Socio socio) throws SQLException {
        if (socio.getId() > 0) {
            return Consultas.insertar(conexion, """
                    INSERT INTO socio (id, dni, nombres, apellidos, telefono, fecha_registro, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, socio.getId(), socio.getDni(), socio.getNombres(), socio.getApellidos(), socio.getTelefono(),
                    socio.getFechaRegistro().toString(), socio.getEstado().name());
        }
        return Consultas.insertar(conexion, """
                INSERT INTO socio (dni, nombres, apellidos, telefono, fecha_registro, estado)
                VALUES (?, ?, ?, ?, ?, ?)
                """, socio.getDni(), socio.getNombres(), socio.getApellidos(), socio.getTelefono(),
                socio.getFechaRegistro().toString(), socio.getEstado().name());
    }

    public void actualizar(Connection conexion, Socio socio) throws SQLException {
        Consultas.actualizar(conexion, """
                UPDATE socio SET dni = ?, nombres = ?, apellidos = ?, telefono = ?, estado = ? WHERE id = ?
                """, socio.getDni(), socio.getNombres(), socio.getApellidos(), socio.getTelefono(), socio.getEstado().name(), socio.getId());
    }

    public boolean tieneHistorial(Connection conexion, int id) throws SQLException {
        return Consultas.contar(conexion, """
                SELECT EXISTS(SELECT 1 FROM membresia WHERE socio_id = ?)
                    OR EXISTS(SELECT 1 FROM reserva_clase WHERE socio_id = ?)
                    OR EXISTS(SELECT 1 FROM asistencia WHERE socio_id = ?)
                """, id, id, id) != 0;
    }

    public void eliminar(Connection conexion, int id) throws SQLException {
        Consultas.actualizar(conexion, "DELETE FROM socio WHERE id = ?", id);
    }
}
