package modelo.persistencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import modelo.Asistencia;
import modelo.ClaseGrupal;
import modelo.ReservaClase;

public final class ActividadDAO {
    private static final String ASISTENCIAS = """
            SELECT a.*, s.nombres || ' ' || s.apellidos AS socio, d.nombre AS sede
            FROM asistencia a JOIN socio s ON s.id = a.socio_id JOIN sede d ON d.id = a.sede_id
            """;
    private static final String CLASES = """
            SELECT c.*, e.nombres || ' ' || e.apellidos AS entrenador, s.nombre AS sede,
                (SELECT COUNT(*) FROM reserva_clase r WHERE r.clase_id = c.id) inscritos
            FROM clase_grupal c JOIN entrenador e ON e.id = c.entrenador_id JOIN sede s ON s.id = c.sede_id
            """;

    private static Asistencia leerAsistencia(ResultSet datos) throws SQLException {
        String salida = datos.getString("salida");
        return new Asistencia(datos.getInt("id"), datos.getInt("socio_id"), datos.getInt("membresia_id"),
                datos.getInt("sede_id"), datos.getString("socio"), datos.getString("sede"),
                LocalDateTime.parse(datos.getString("entrada")), salida == null ? null : LocalDateTime.parse(salida));
    }

    public List<Asistencia> asistencias(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, ASISTENCIAS + " ORDER BY a.id DESC", ActividadDAO::leerAsistencia);
    }

    public Asistencia asistenciaAbierta(Connection conexion, int socio) throws SQLException {
        return Consultas.uno(conexion, ASISTENCIAS + " WHERE a.socio_id = ? AND a.salida IS NULL",
                ActividadDAO::leerAsistencia, socio);
    }

    public void entrada(Connection conexion, int socio, int membresia, int sede, LocalDateTime hora)
            throws SQLException {
        Consultas.actualizar(conexion, """
                INSERT INTO asistencia (socio_id, membresia_id, sede_id, entrada) VALUES (?, ?, ?, ?)
                """, socio, membresia, sede, hora.toString());
    }

    public void salida(Connection conexion, int asistencia, LocalDateTime hora) throws SQLException {
        Consultas.actualizar(conexion, "UPDATE asistencia SET salida = ? WHERE id = ?", hora.toString(), asistencia);
    }

    private static ClaseGrupal leerClase(ResultSet datos) throws SQLException {
        return new ClaseGrupal(datos.getInt("id"), datos.getString("nombre"), datos.getString("entrenador"),
                datos.getString("sede"), LocalDateTime.parse(datos.getString("horario")),
                datos.getInt("capacidad"), datos.getInt("inscritos"));
    }

    public List<ClaseGrupal> clases(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, CLASES + " ORDER BY c.horario, c.id", ActividadDAO::leerClase);
    }

    public ClaseGrupal clase(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, CLASES + " WHERE c.id = ?", ActividadDAO::leerClase, id);
    }

    public int capacidadClasesFuturas(Connection conexion, int sede, LocalDateTime ahora) throws SQLException {
        return Consultas.contar(conexion,
                "SELECT COALESCE(MAX(capacidad), 0) FROM clase_grupal WHERE sede_id = ? AND horario > ?", sede,
                ahora.toString());
    }

    public int guardarClase(Connection conexion, String nombre, int entrenador, int sede,
                           LocalDateTime horario, int capacidad) throws SQLException {
        return Consultas.insertar(conexion, """
                INSERT INTO clase_grupal (nombre, entrenador_id, sede_id, horario, capacidad) VALUES (?, ?, ?, ?, ?)
                """, nombre, entrenador, sede, horario.toString(), capacidad);
    }

    public boolean existeReserva(Connection conexion, int clase, int socio) throws SQLException {
        return Consultas.contar(conexion,
                "SELECT COUNT(*) FROM reserva_clase WHERE clase_id = ? AND socio_id = ?", clase, socio) > 0;
    }

    public void reservar(Connection conexion, int clase, int socio, LocalDateTime fecha) throws SQLException {
        Consultas.actualizar(conexion, "INSERT INTO reserva_clase (clase_id, socio_id, fecha) VALUES (?, ?, ?)",
                clase, socio, fecha.toString());
    }

    public boolean cancelar(Connection conexion, int clase, int socio) throws SQLException {
        return Consultas.actualizar(conexion, "DELETE FROM reserva_clase WHERE clase_id = ? AND socio_id = ?",
                clase, socio) == 1;
    }

    public List<ReservaClase> reservas(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, """
                SELECT r.*, s.nombres || ' ' || s.apellidos || ' · ' || s.dni AS socio
                FROM reserva_clase r JOIN socio s ON s.id = r.socio_id ORDER BY s.apellidos, s.nombres
                """, datos -> new ReservaClase(datos.getInt("clase_id"), datos.getInt("socio_id"),
                        datos.getString("socio"), LocalDateTime.parse(datos.getString("fecha"))));
    }
}
