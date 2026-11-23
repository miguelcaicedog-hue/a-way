ALTER TABLE player
    ADD COLUMN reemplazo boolean NOT NULL DEFAULT false;

CREATE TABLE refuerzo_archivo (
    id_refuerzo_archivo uuid PRIMARY KEY,
    id_liga uuid NOT NULL REFERENCES league (id),
    id_edicion uuid NOT NULL REFERENCES edition (id),
    nombre_archivo varchar(255) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT refuerzo_archivo_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'))
) TABLESPACE ts_away;

CREATE TABLE refuerzo_equipo (
    id_refuerzo_equipo uuid PRIMARY KEY,
    id_refuerzo_archivo uuid NOT NULL REFERENCES refuerzo_archivo (id_refuerzo_archivo),
    numero_fila integer NOT NULL,
    categoria varchar(160) NOT NULL,
    nombre_equipo varchar(160) NOT NULL,
    seleccionado boolean NOT NULL DEFAULT true,
    resultado varchar(20),
    detalle text,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT refuerzo_equipo_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT refuerzo_equipo_resultado_check CHECK (resultado IS NULL OR resultado IN ('OK', 'ERROR', 'SIN CARGA')),
    CONSTRAINT refuerzo_equipo_fila_key UNIQUE (id_refuerzo_archivo, numero_fila)
) TABLESPACE ts_away;

CREATE INDEX refuerzo_equipo_archivo_idx
    ON refuerzo_equipo (id_refuerzo_archivo)
    TABLESPACE ts_away;

CREATE TABLE refuerzo_jugador (
    id_refuerzo_jugador uuid PRIMARY KEY,
    id_refuerzo_equipo uuid NOT NULL REFERENCES refuerzo_equipo (id_refuerzo_equipo),
    orden integer NOT NULL,
    numero_camisa_reemplazo varchar(40),
    cedula varchar(80),
    nombre varchar(200),
    numero_camisa varchar(40),
    url_foto text,
    foto bytea,
    tipo_foto varchar(80),
    alias varchar(80),
    cargado boolean NOT NULL DEFAULT false,
    existente boolean NOT NULL DEFAULT false,
    foto_cargada boolean NOT NULL DEFAULT false,
    motivo text,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT refuerzo_jugador_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT refuerzo_jugador_orden_key UNIQUE (id_refuerzo_equipo, orden)
) TABLESPACE ts_away;

CREATE INDEX refuerzo_jugador_equipo_idx
    ON refuerzo_jugador (id_refuerzo_equipo)
    TABLESPACE ts_away;

CREATE TRIGGER refuerzo_archivo_modificacion
    BEFORE UPDATE ON refuerzo_archivo
    FOR EACH ROW EXECUTE PROCEDURE actualizar_fecha_modificacion();

CREATE TRIGGER refuerzo_equipo_modificacion
    BEFORE UPDATE ON refuerzo_equipo
    FOR EACH ROW EXECUTE PROCEDURE actualizar_fecha_modificacion();

CREATE TRIGGER refuerzo_jugador_modificacion
    BEFORE UPDATE ON refuerzo_jugador
    FOR EACH ROW EXECUTE PROCEDURE actualizar_fecha_modificacion();
