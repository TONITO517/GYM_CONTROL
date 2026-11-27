package modelo.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import modelo.Membresia.Congelamiento;
import modelo.Membresia;
import modelo.Pago;
import modelo.Socio;
import modelo.persistencia.ConexionSqlite.Consultas;

public final class SocioMembresiaDAO {
    // Consulta de membresías
    private static final String MEMBRESIAS = """
            SELECT m.*, s.nombres || ' ' || s.apellidos AS socio, p.nombre AS plan,
                COALESCE((SELECT SUM(a.monto) FROM pago a WHERE a.membresia_id = m.id), 0) AS pagado,
                c.id AS pausa_id, c.inicio AS pausa_inicio, c.fin AS pausa_fin, c.motivo AS pausa_motivo
            FROM membresia m JOIN socio s ON s.id = m.socio_id JOIN plan p ON p.id = m.plan_id
            LEFT JOIN congelamiento c ON c.id = (
                SELECT id FROM congelamiento WHERE membresia_id = m.id ORDER BY id DESC LIMIT 1
            )
            """;

    // Conversión de ResultSet a Membresia
    private static Membresia leer(ResultSet datos) throws SQLException {
        Congelamiento pausa = null;
        int pausaId = datos.getInt("pausa_id");
        if (!datos.wasNull()) {
            pausa = new Congelamiento(pausaId, datos.getInt("id"), LocalDate.parse(datos.getString("pausa_inicio")),
                    LocalDate.parse(datos.getString("pausa_fin")), datos.getString("pausa_motivo"));
        }
        return new Membresia(datos.getInt("id"), datos.getInt("socio_id"), datos.getInt("plan_id"),
                datos.getString("socio"), datos.getString("plan"), LocalDate.parse(datos.getString("inicio")),
                LocalDate.parse(datos.getString("vencimiento")), datos.getInt("precio"), datos.getInt("pagado"),
                datos.getString("tarifa"), pausa);
    }

    // Búsquedas de membresías
    public List<Membresia> listar(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, MEMBRESIAS + " ORDER BY m.id DESC", fila -> SocioMembresiaDAO.leer(fila));
    }

    public List<Membresia> porSocio(Connection conexion, int socioId) throws SQLException {
        return Consultas.listar(conexion, MEMBRESIAS + " WHERE m.socio_id = ? ORDER BY m.inicio DESC",
                fila -> SocioMembresiaDAO.leer(fila), socioId);
    }

    public Membresia porId(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, MEMBRESIAS + " WHERE m.id = ?", fila -> SocioMembresiaDAO.leer(fila), id);
    }

    // Registro de membresías (INSERT)
    public int guardar(Connection conexion, int socio, int plan, LocalDate inicio, LocalDate fin,
                       int precio, String tarifa) throws SQLException {
        return Consultas.insertar(conexion, """
                INSERT INTO membresia (socio_id, plan_id, inicio, vencimiento, precio, tarifa)
                VALUES (?, ?, ?, ?, ?, ?)
                """, socio, plan, inicio.toString(), fin.toString(), precio, tarifa);
    }

    // Restauración y eliminación de membresías
    public void restaurar(Connection conexion, Membresia membresia) throws SQLException {
        Consultas.actualizar(conexion, """
                INSERT INTO membresia (id, socio_id, plan_id, inicio, vencimiento, precio, tarifa)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, membresia.getId(), membresia.getSocioId(), membresia.getPlanId(),
                membresia.getInicio().toString(), membresia.getVencimiento().toString(),
                membresia.getPrecio(), membresia.getTarifa());
    }

    public boolean tieneDependencias(Connection conexion, int id) throws SQLException {
        return Consultas.contar(conexion, """
                SELECT EXISTS(SELECT 1 FROM congelamiento WHERE membresia_id = ?)
                    OR EXISTS(SELECT 1 FROM pago WHERE membresia_id = ?)
                """, id, id) != 0;
    }

    public void eliminar(Connection conexion, int id) throws SQLException {
        Consultas.actualizar(conexion, "DELETE FROM membresia WHERE id = ?", id);
    }

    // Congelamiento y reactivación
    public void congelar(Connection conexion, int id, LocalDate inicio, LocalDate fin, String motivo,
                         LocalDate vencimiento) throws SQLException {
        Consultas.actualizar(conexion, """
                INSERT INTO congelamiento (membresia_id, inicio, fin, motivo) VALUES (?, ?, ?, ?)
                """, id, inicio.toString(), fin.toString(), motivo);
        actualizarVencimiento(conexion, id, vencimiento);
    }

    public void reactivar(Connection conexion, Congelamiento pausa, LocalDate fin, LocalDate vencimiento)
            throws SQLException {
        Consultas.actualizar(conexion, "UPDATE congelamiento SET fin = ? WHERE id = ?", fin.toString(), pausa.getId());
        actualizarVencimiento(conexion, pausa.getMembresiaId(), vencimiento);
    }

    private void actualizarVencimiento(Connection conexion, int id, LocalDate vencimiento) throws SQLException {
        Consultas.actualizar(conexion, "UPDATE membresia SET vencimiento = ? WHERE id = ?", vencimiento.toString(), id);
    }

    // Registro y consultas de pagos
    public int abonar(Connection conexion, int id, LocalDateTime fecha, int monto, Pago.Metodo metodo)
            throws SQLException {
        return Consultas.insertar(conexion, "INSERT INTO pago (membresia_id, fecha, monto, metodo) VALUES (?, ?, ?, ?)",
                id, fecha.toString(), monto, metodo.name());
    }

    public Pago pago(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, "SELECT * FROM pago WHERE id = ?", datos ->
                new Pago(datos.getInt("id"), datos.getInt("membresia_id"),
                        LocalDateTime.parse(datos.getString("fecha")), datos.getInt("monto"),
                        Pago.Metodo.valueOf(datos.getString("metodo"))), id);
    }

    // Restauración y eliminación de pagos
    public void restaurarPago(Connection conexion, Pago pago) throws SQLException {
        Consultas.actualizar(conexion, """
                INSERT INTO pago (id, membresia_id, fecha, monto, metodo) VALUES (?, ?, ?, ?, ?)
                """, pago.getId(), pago.getMembresiaId(), pago.getFecha().toString(),
                pago.getMonto(), pago.getMetodo().name());
    }

    public void eliminarPago(Connection conexion, int id) throws SQLException {
        Consultas.actualizar(conexion, "DELETE FROM pago WHERE id = ?", id);
    }

    public List<Pago> pagos(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, "SELECT * FROM pago ORDER BY id DESC",
                datos -> new Pago(datos.getInt("id"), datos.getInt("membresia_id"),
                        LocalDateTime.parse(datos.getString("fecha")), datos.getInt("monto"),
                        Pago.Metodo.valueOf(datos.getString("metodo"))));
    }

    // Consultas de socios

    private static Socio leerSocio(ResultSet datos) throws SQLException {
        return new Socio(datos.getInt("id"), datos.getString("dni"), datos.getString("nombres"),
                datos.getString("apellidos"), datos.getString("telefono"),
                LocalDate.parse(datos.getString("fecha_registro")), Socio.Estado.valueOf(datos.getString("estado")));
    }

    public List<Socio> listarSocios(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, "SELECT * FROM socio ORDER BY apellidos, nombres, id", fila -> SocioMembresiaDAO.leerSocio(fila));
    }

    public Socio socioPorId(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, "SELECT * FROM socio WHERE id = ?", fila -> SocioMembresiaDAO.leerSocio(fila), id);
    }

    public Socio socioPorDni(Connection conexion, String dni)
            throws SQLException {
        String sql = "SELECT * FROM socio WHERE dni = ?";
        try (PreparedStatement consulta =
                conexion.prepareStatement(sql)) {
            consulta.setString(1, dni);
            try (ResultSet filas = consulta.executeQuery()) {
                if (filas.next()) {
                    return leerSocio(filas);
                }
            }
        }
        return null;
    }

    // Registro de socios
    public int insertarSocio(Connection conexion, Socio socio) throws SQLException {
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

    // Actualización de socios
    public void actualizarSocio(Connection conexion, Socio socio) throws SQLException {
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

    public void eliminarSocio(Connection conexion, int id) throws SQLException {
        Consultas.actualizar(conexion, "DELETE FROM socio WHERE id = ?", id);
    }
}
