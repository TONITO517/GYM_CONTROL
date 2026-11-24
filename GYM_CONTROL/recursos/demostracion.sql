INSERT INTO sede (id, nombre, direccion, capacidad) VALUES
    (1, 'Arequipa Centro', 'Calle Ejemplo 120', 25),
    (2, 'Bustamante', 'Av. Ejemplo 240', 16);

INSERT INTO socio (id, dni, nombres, apellidos, telefono, fecha_registro, estado) VALUES
    (1, '00000011', 'Camila', 'Salas', '900000011', date('now', 'localtime', '-20 days'), 'ACTIVO'),
    (2, '00000022', 'Diego', 'Rivas', '900000022', date('now', 'localtime', '-25 days'), 'ACTIVO'),
    (3, '00000033', 'Lucía', 'Vega', '900000033', date('now', 'localtime', '-10 days'), 'ACTIVO'),
    (4, '00000044', 'Mateo', 'Flores', '900000044', date('now', 'localtime', '-8 days'), 'SUSPENDIDO'),
    (5, '00000055', 'Valeria', 'Torres', '900000055', date('now', 'localtime', '-1 day'), 'ACTIVO');

INSERT INTO plan (id, nombre, meses, precio) VALUES
    (1, 'Mensual Centro', 1, 15000),
    (2, 'Trimestral Multisede', 3, 39000),
    (3, 'Semestral Multisede', 6, 72000);

INSERT INTO plan_sede (plan_id, sede_id) VALUES (1, 1), (2, 1), (2, 2), (3, 1), (3, 2);

INSERT INTO membresia (id, socio_id, plan_id, inicio, vencimiento, precio, tarifa) VALUES
    (1, 1, 1, date('now', 'localtime', '-15 days'), date('now', 'localtime', '-15 days', '+1 month', '-1 day'), 15000, 'Tarifa normal'),
    (2, 2, 1, date('now', 'localtime', '-25 days'), date('now', 'localtime', '-25 days', '+1 month', '-1 day'), 15000, 'Tarifa normal'),
    (3, 3, 2, date('now', 'localtime', '-10 days'), date('now', 'localtime', '-10 days', '+3 months', '+4 days'), 39000, 'Tarifa normal');

INSERT INTO congelamiento (membresia_id, inicio, fin, motivo)
    VALUES (3, date('now', 'localtime', '-1 day'), date('now', 'localtime', '+4 days'), 'Viaje');

INSERT INTO pago (membresia_id, fecha, monto, metodo) VALUES
    (1, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-15 days'), 10000, 'YAPE'),
    (2, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-25 days'), 15000, 'EFECTIVO'),
    (3, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-10 days'), 20000, 'TARJETA');

INSERT INTO asistencia (socio_id, membresia_id, sede_id, entrada, salida)
    VALUES (1, 1, 1, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-1 day', '-1 hour'),
            strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-1 day'));

INSERT INTO entrenador (id, nombres, apellidos, especialidad, telefono, disponibilidad) VALUES
    (1, 'Andrea', 'Castro', 'Entrenamiento funcional', '900000101', 'Lunes a viernes, tardes'),
    (2, 'Renzo', 'Medina', 'Yoga', '900000102', 'Martes y jueves, mañanas');

INSERT INTO clase_grupal (id, nombre, entrenador_id, sede_id, horario, capacidad) VALUES
    (1, 'Funcional', 1, 1, date('now', 'localtime', '+1 day') || 'T18:30', 8),
    (2, 'Yoga', 2, 2, date('now', 'localtime', '+2 days') || 'T09:00', 6);

INSERT INTO reserva_clase (clase_id, socio_id, fecha)
    VALUES (1, 2, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime'));
