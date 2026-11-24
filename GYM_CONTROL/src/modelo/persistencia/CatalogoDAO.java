package modelo.persistencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import modelo.Entrenador;
import modelo.Plan;
import modelo.Sede;

public final class CatalogoDAO {
    private static final String SEDES = """
            SELECT s.*, (SELECT COUNT(*) FROM asistencia a WHERE a.sede_id = s.id AND a.salida IS NULL) presentes
            FROM sede s
            """;

    private static Sede leerSede(ResultSet datos) throws SQLException {
        return new Sede(datos.getInt("id"), datos.getString("nombre"), datos.getString("direccion"),
                datos.getInt("capacidad"), datos.getInt("presentes"));
    }

    public List<Sede> sedes(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, SEDES + " ORDER BY s.nombre", CatalogoDAO::leerSede);
    }

    public Sede sede(Connection conexion, int id) throws SQLException {
        return Consultas.uno(conexion, SEDES + " WHERE s.id = ?", CatalogoDAO::leerSede, id);
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

    private static Entrenador leerEntrenador(ResultSet datos) throws SQLException {
        return new Entrenador(datos.getInt("id"), datos.getString("nombres"), datos.getString("apellidos"),
                datos.getString("especialidad"), datos.getString("telefono"), datos.getString("disponibilidad"));
    }

    public List<Entrenador> entrenadores(Connection conexion) throws SQLException {
        return Consultas.listar(conexion, "SELECT * FROM entrenador ORDER BY apellidos, nombres",
                CatalogoDAO::leerEntrenador);
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
