CREATE TABLE inscripcion_archivo (
    id_inscripcion_archivo uuid PRIMARY KEY,
    id_liga uuid NOT NULL REFERENCES league (id),
    id_edicion uuid NOT NULL REFERENCES edition (id),
    nombre_archivo varchar(255) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT inscripcion_archivo_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'))
) TABLESPACE ts_away;

CREATE TABLE inscripcion_equipo (
    id_inscripcion_equipo uuid PRIMARY KEY,
    id_inscripcion_archivo uuid NOT NULL REFERENCES inscripcion_archivo (id_inscripcion_archivo),
    numero_fila integer NOT NULL,
    categoria varchar(160) NOT NULL,
    nombre_equipo varchar(160) NOT NULL,
    nombre_representante varchar(160),
    numero_telefono varchar(80),
    seleccionado boolean NOT NULL DEFAULT true,
    resultado varchar(20),
    detalle text,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT inscripcion_equipo_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT inscripcion_equipo_resultado_check CHECK (resultado IS NULL OR resultado IN ('OK', 'ERROR', 'SIN CARGA')),
    CONSTRAINT inscripcion_equipo_fila_key UNIQUE (id_inscripcion_archivo, numero_fila)
) TABLESPACE ts_away;

CREATE INDEX inscripcion_equipo_archivo_idx
    ON inscripcion_equipo (id_inscripcion_archivo)
    TABLESPACE ts_away;

CREATE TABLE inscripcion_jugador (
    id_inscripcion_jugador uuid PRIMARY KEY,
    id_inscripcion_equipo uuid NOT NULL REFERENCES inscripcion_equipo (id_inscripcion_equipo),
    orden integer NOT NULL,
    cedula varchar(80),
    nombre varchar(200),
    numero_camisa varchar(40),
    url_foto text,
    alias varchar(80),
    cargado boolean NOT NULL DEFAULT false,
    foto_cargada boolean NOT NULL DEFAULT false,
    motivo text,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT inscripcion_jugador_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT inscripcion_jugador_orden_key UNIQUE (id_inscripcion_equipo, orden)
) TABLESPACE ts_away;

CREATE INDEX inscripcion_jugador_equipo_idx
    ON inscripcion_jugador (id_inscripcion_equipo)
    TABLESPACE ts_away;

CREATE OR REPLACE FUNCTION actualizar_fecha_modificacion()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.fecha_creacion = OLD.fecha_creacion;
    NEW.fecha_modificacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER inscripcion_archivo_modificacion
    BEFORE UPDATE ON inscripcion_archivo
    FOR EACH ROW EXECUTE PROCEDURE actualizar_fecha_modificacion();

CREATE TRIGGER inscripcion_equipo_modificacion
    BEFORE UPDATE ON inscripcion_equipo
    FOR EACH ROW EXECUTE PROCEDURE actualizar_fecha_modificacion();

CREATE TRIGGER inscripcion_jugador_modificacion
    BEFORE UPDATE ON inscripcion_jugador
    FOR EACH ROW EXECUTE PROCEDURE actualizar_fecha_modificacion();
