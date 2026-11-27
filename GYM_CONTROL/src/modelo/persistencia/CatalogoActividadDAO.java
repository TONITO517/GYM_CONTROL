package modelo.persistencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import modelo.Asistencia;
import modelo.ClaseGrupal.Entrenador;
import modelo.ClaseGrupal.ReservaClase;
import modelo.ClaseGrupal;
import modelo.Plan;
import modelo.Sede;
import modelo.persistencia.ConexionSqlite.Consultas;

public final class CatalogoActividadDAO {
    // Consultas de asistencias y clases
    private static final String ASISTENCIAS = """
            SELECT a.*, s.nombres || ' ' || s.apellidos AS socio, d.nombre AS sede
            FROM asistencia a JOIN socio s ON s.id = a.socio_id JOIN sede d ON d.id = a.sede_id
            """;
    private static final String CLASES = """
            SELECT c.*, e.nombres || ' ' || e.apellidos AS entrenador, s.nombre AS sede,
                (SELECT COUNT(*) FROM reserva_clase r WHERE r.clase_id = c.id) inscritos
            FROM clase_grupal c JOIN entrenador e ON e.id = c.entrenador_id JOIN sede s ON s.id = c.sede_id
            """;

    // Lectura de asistencias
    private static Asistencia leerAsistencia(ResultSet datos) throws SQLException {
        String salida = datos.getString("salida");
        return new Asistencia(datos.getInt("id"), datos.getInt("socio_id"), datos.getInt("membresia_id"),
                datos.getInt("sede_id"), datos.getString("socio"), datos.getString("sede"),
                LocalDateTime.parse(datos.getString("entrada")), salida == null ? null : LocalDateTime.parse(salida));
    }

    public List<Asistencia> asistencias(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, ASISTENCIAS + " ORDER BY a.id DESC", fila -> CatalogoActividadDAO.leerAsistencia(fila));
    }

    public Asistencia asistenciaAbierta(Connection conexion, int socio) throws SQLException {
        return Consultas.uno(conexion, ASISTENCIAS + " WHERE a.socio_id = ? AND a.salida IS NULL",
                fila -> CatalogoActividadDAO.leerAsistencia(fila), socio);
    }

    // Registro de entrada y salida
    public void entrada(Connection conexion, int socio, int membresia, int sede, LocalDateTime hora)
            throws SQLException {
        Consultas.actualizar(conexion, """
                INSERT INTO asistencia (socio_id, membresia_id, sede_id, entrada) VALUES (?, ?, ?, ?)
                """, socio, membresia, sede, hora.toString());
    }

    public void salida(Connection conexion, int asistencia, LocalDateTime hora) throws SQLException {
        Consultas.actualizar(conexion, "UPDATE asistencia SET salida = ? WHERE id = ?", hora.toString(), asistencia);
    }

    public boolean tieneAsistenciaMembresia(Connection conexion, int membresiaId) throws SQLException {
        return Consultas.contar(conexion, "SELECT EXISTS(SELECT 1 FROM asistencia WHERE membresia_id = ?)",
                membresiaId) != 0;
    }

    // Consulta de clases grupales
    private static ClaseGrupal leerClase(ResultSet datos) throws SQLException {
        return new ClaseGrupal(datos.getInt("id"), datos.getString("nombre"), datos.getString("entrenador"),
                datos.getString("sede"), LocalDateTime.parse(datos.getString("horario")),
                datos.getInt("capacidad"), datos.getInt("inscritos"));
    }

    public List<ClaseGrupal> clases(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, CLASES + " ORDER BY c.horario, c.id", fila -> CatalogoActividadDAO.leerClase(fila));
    }

    public ClaseGrupal clase(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, CLASES + " WHERE c.id = ?", fila -> CatalogoActividadDAO.leerClase(fila), id);
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

    // Reservas y cancelaciones
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

    // Catálogo de sedes, planes y entrenadores

    private static final String SEDES = """
            SELECT s.*, (SELECT COUNT(*) FROM asistencia a WHERE a.sede_id = s.id AND a.salida IS NULL) presentes
            FROM sede s
            """;

    private static Sede leerSede(ResultSet datos) throws SQLException {
        return new Sede(datos.getInt("id"), datos.getString("nombre"), datos.getString("direccion"),
                datos.getInt("capacidad"), datos.getInt("presentes"));
    }

    public List<Sede> sedes(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, SEDES + " ORDER BY s.nombre", fila -> CatalogoActividadDAO.leerSede(fila));
    }

    public Sede sede(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, SEDES + " WHERE s.id = ?", fila -> CatalogoActividadDAO.leerSede(fila), id);
    }

    public boolean sedeRepetida(Connection conexion, String nombre, int id) throws SQLException {
        return Consultas.contar(conexion, "SELECT COUNT(*) FROM sede WHERE nombre = ? AND id <> ?", nombre, id) > 0;
    }

    public int guardarSede(Connection conexion, int id, String nombre, String direccion, int capacidad)
            throws SQLException {
        if (id == 0) {
            return Consultas.insertar(conexion, "INSERT INTO sede (nombre, direccion, capacidad) VALUES (?, ?, ?)",
                    nombre, direccion, capacidad);
        }
        Consultas.actualizar(conexion, "UPDATE sede SET nombre = ?, direccion = ?, capacidad = ? WHERE id = ?",
                nombre, direccion, capacidad, id);
        return id;
    }

    // La tabla intermedia guarda las sedes autorizadas para cada plan.
    private Plan leerPlan(Connection conexion, ResultSet datos) throws SQLException {
        int planId = datos.getInt("id");
        String sql = "SELECT sede_id FROM plan_sede WHERE plan_id = ? ORDER BY sede_id";
        List<Integer> sedes = Consultas.listar(conexion, sql, fila -> fila.getInt(1), planId);
        return new Plan(planId, datos.getString("nombre"), datos.getInt("meses"), datos.getInt("precio"), sedes);
    }

    public List<Plan> planes(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, "SELECT * FROM plan ORDER BY nombre", datos -> leerPlan(conexion, datos));
    }

    public Plan plan(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, "SELECT * FROM plan WHERE id = ?", datos -> leerPlan(conexion, datos), id);
    }

    public boolean planRepetido(Connection conexion, String nombre, int id) throws SQLException {
        return Consultas.contar(conexion, "SELECT COUNT(*) FROM plan WHERE nombre = ? AND id <> ?", nombre, id) > 0;
    }

    public int guardarPlan(Connection conexion, int id, String nombre, int meses, int precio, List<Integer> sedes)
            throws SQLException {
        int planId = id;
        if (id == 0) {
            planId = Consultas.insertar(conexion, "INSERT INTO plan (nombre, meses, precio) VALUES (?, ?, ?)",
                    nombre, meses, precio);
        } else {
            Consultas.actualizar(conexion, "UPDATE plan SET nombre = ?, meses = ?, precio = ? WHERE id = ?",
                    nombre, meses, precio, id);
            Consultas.actualizar(conexion, "DELETE FROM plan_sede WHERE plan_id = ?", id);
        }
        for (int sedeId : sedes) {
            Consultas.actualizar(conexion, "INSERT INTO plan_sede (plan_id, sede_id) VALUES (?, ?)", planId, sedeId);
        }
        return planId;
    }

    // Gestión de entrenadores
    private static Entrenador leerEntrenador(ResultSet datos) throws SQLException {
        return new Entrenador(datos.getInt("id"), datos.getString("nombres"), datos.getString("apellidos"),
                datos.getString("especialidad"), datos.getString("telefono"), datos.getString("disponibilidad"));
    }

    public List<Entrenador> entrenadores(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, "SELECT * FROM entrenador ORDER BY apellidos, nombres",
                fila -> CatalogoActividadDAO.leerEntrenador(fila));
    }

    public boolean existeEntrenador(Connection conexion, int id) throws SQLException {
        return Consultas.contar(conexion, "SELECT COUNT(*) FROM entrenador WHERE id = ?", id) > 0;
    }

    public int guardarEntrenador(Connection conexion, Entrenador entrenador) throws SQLException {
        if (entrenador.getId() == 0) {
            return Consultas.insertar(conexion, """
                    INSERT INTO entrenador (nombres, apellidos, especialidad, telefono, disponibilidad)
                    VALUES (?, ?, ?, ?, ?)
                    """, entrenador.getNombres(), entrenador.getApellidos(), entrenador.getEspecialidad(),
                    entrenador.getTelefono(), entrenador.getDisponibilidad());
        }
        Consultas.actualizar(conexion, """
                UPDATE entrenador SET nombres = ?, apellidos = ?, especialidad = ?, telefono = ?, disponibilidad = ?
                WHERE id = ?
                """, entrenador.getNombres(), entrenador.getApellidos(), entrenador.getEspecialidad(),
                entrenador.getTelefono(), entrenador.getDisponibilidad(), entrenador.getId());
        return entrenador.getId();
    }
}
