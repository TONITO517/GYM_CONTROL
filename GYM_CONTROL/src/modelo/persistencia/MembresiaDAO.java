package modelo.persistencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import modelo.Congelamiento;
import modelo.Membresia;
import modelo.Pago;

public final class MembresiaDAO {
    private static final String MEMBRESIAS = """
            SELECT m.*, s.nombres || ' ' || s.apellidos AS socio, p.nombre AS plan,
                COALESCE((SELECT SUM(a.monto) FROM pago a WHERE a.membresia_id = m.id), 0) AS pagado,
                c.id AS pausa_id, c.inicio AS pausa_inicio, c.fin AS pausa_fin, c.motivo AS pausa_motivo
            FROM membresia m JOIN socio s ON s.id = m.socio_id JOIN plan p ON p.id = m.plan_id
            LEFT JOIN congelamiento c ON c.id = (
                SELECT id FROM congelamiento WHERE membresia_id = m.id ORDER BY id DESC LIMIT 1
            )
            """;

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

    public List<Membresia> listar(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, MEMBRESIAS + " ORDER BY m.id DESC", MembresiaDAO::leer);
    }

    public List<Membresia> porSocio(Connection conexion, int socioId) throws SQLException {
        return Consultas.listar(conexion, MEMBRESIAS + " WHERE m.socio_id = ? ORDER BY m.inicio DESC",
                MembresiaDAO::leer, socioId);
    }

    public Membresia porId(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, MEMBRESIAS + " WHERE m.id = ?", MembresiaDAO::leer, id);
    }

    public int guardar(Connection conexion, int socio, int plan, LocalDate inicio, LocalDate fin,
                       int precio, String tarifa) throws SQLException {
        return Consultas.insertar(conexion, """
                INSERT INTO membresia (socio_id, plan_id, inicio, vencimiento, precio, tarifa)
                VALUES (?, ?, ?, ?, ?, ?)
                """, socio, plan, inicio.toString(), fin.toString(), precio, tarifa);
    }

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

    public void abonar(Connection conexion, int id, LocalDateTime fecha, int monto, Pago.Metodo metodo)
            throws SQLException {
        Consultas.actualizar(conexion, "INSERT INTO pago (membresia_id, fecha, monto, metodo) VALUES (?, ?, ?, ?)",
                id, fecha.toString(), monto, metodo.name());
    }

    public List<Pago> pagos(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, "SELECT * FROM pago ORDER BY id DESC",
                datos -> new Pago(datos.getInt("id"), datos.getInt("membresia_id"),
                        LocalDateTime.parse(datos.getString("fecha")), datos.getInt("monto"),
                        Pago.Metodo.valueOf(datos.getString("metodo"))));
    }
}
