CREATE TABLE IF NOT EXISTS socio (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni TEXT NOT NULL UNIQUE CHECK(length(dni) = 8),
    nombres TEXT NOT NULL,
    apellidos TEXT NOT NULL,
    telefono TEXT NOT NULL,
    fecha_registro TEXT NOT NULL,
    estado TEXT NOT NULL CHECK(estado IN ('ACTIVO', 'SUSPENDIDO'))
);

CREATE TABLE IF NOT EXISTS sede (
    id INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL COLLATE NOCASE UNIQUE,
    direccion TEXT NOT NULL,
    capacidad INTEGER NOT NULL CHECK(capacidad > 0)
);

CREATE TABLE IF NOT EXISTS plan (
    id INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL COLLATE NOCASE UNIQUE,
    meses INTEGER NOT NULL CHECK(meses BETWEEN 1 AND 36),
    precio INTEGER NOT NULL CHECK(precio > 0)
);

CREATE TABLE IF NOT EXISTS plan_sede (
    plan_id INTEGER NOT NULL REFERENCES plan(id),
    sede_id INTEGER NOT NULL REFERENCES sede(id),
    PRIMARY KEY(plan_id, sede_id)
);

CREATE TABLE IF NOT EXISTS membresia (
    id INTEGER PRIMARY KEY,
    socio_id INTEGER NOT NULL REFERENCES socio(id),
    plan_id INTEGER NOT NULL REFERENCES plan(id),
    inicio TEXT NOT NULL,
    vencimiento TEXT NOT NULL CHECK(vencimiento >= inicio),
    precio INTEGER NOT NULL CHECK(precio > 0),
    tarifa TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS congelamiento (
    id INTEGER PRIMARY KEY,
    membresia_id INTEGER NOT NULL REFERENCES membresia(id),
    inicio TEXT NOT NULL,
    fin TEXT NOT NULL CHECK(fin >= inicio),
    motivo TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS pago (
    id INTEGER PRIMARY KEY,
    membresia_id INTEGER NOT NULL REFERENCES membresia(id),
    fecha TEXT NOT NULL,
    monto INTEGER NOT NULL CHECK(monto > 0),
    metodo TEXT NOT NULL CHECK(metodo IN ('EFECTIVO', 'TARJETA', 'YAPE', 'PLIN', 'TRANSFERENCIA'))
);

CREATE TABLE IF NOT EXISTS asistencia (
    id INTEGER PRIMARY KEY,
    socio_id INTEGER NOT NULL REFERENCES socio(id),
    membresia_id INTEGER NOT NULL REFERENCES membresia(id),
    sede_id INTEGER NOT NULL REFERENCES sede(id),
    entrada TEXT NOT NULL,
    salida TEXT CHECK(salida IS NULL OR salida >= entrada)
);

CREATE UNIQUE INDEX IF NOT EXISTS entrada_abierta_por_socio
    ON asistencia(socio_id) WHERE salida IS NULL;

CREATE TABLE IF NOT EXISTS entrenador (
    id INTEGER PRIMARY KEY,
    nombres TEXT NOT NULL,
    apellidos TEXT NOT NULL,
    especialidad TEXT NOT NULL,
    telefono TEXT NOT NULL,
    disponibilidad TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS clase_grupal (
    id INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL,
    entrenador_id INTEGER NOT NULL REFERENCES entrenador(id),
    sede_id INTEGER NOT NULL REFERENCES sede(id),
    horario TEXT NOT NULL,
    capacidad INTEGER NOT NULL CHECK(capacidad > 0)
);

CREATE TABLE IF NOT EXISTS reserva_clase (
    clase_id INTEGER NOT NULL REFERENCES clase_grupal(id),
    socio_id INTEGER NOT NULL REFERENCES socio(id),
    fecha TEXT NOT NULL,
    PRIMARY KEY(clase_id, socio_id)
);

CREATE INDEX IF NOT EXISTS membresias_por_socio ON membresia(socio_id, inicio);
CREATE INDEX IF NOT EXISTS pagos_por_membresia ON pago(membresia_id);
CREATE INDEX IF NOT EXISTS pausas_por_membresia ON congelamiento(membresia_id, id);
CREATE INDEX IF NOT EXISTS presentes_por_sede ON asistencia(sede_id) WHERE salida IS NULL;
