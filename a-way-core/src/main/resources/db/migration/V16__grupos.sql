CREATE TABLE grupos (
    id_grupo uuid NOT NULL,
    id_categoria uuid NOT NULL REFERENCES category (id),
    nombre varchar(80) NOT NULL,
    orden integer NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT grupos_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT grupos_orden_check CHECK (orden > 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX grupos_pkey ON grupos (id_grupo) TABLESPACE ts_away;
ALTER TABLE grupos ADD CONSTRAINT grupos_pkey PRIMARY KEY USING INDEX grupos_pkey;

CREATE UNIQUE INDEX grupos_categoria_nombre_key
    ON grupos (id_categoria, nombre)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX grupos_categoria_idx
    ON grupos (id_categoria)
    TABLESPACE ts_away;

CREATE TABLE grupos_equipo (
    id_grupo_equipo uuid NOT NULL,
    id_grupo uuid NOT NULL REFERENCES grupos (id_grupo),
    id_categoria uuid NOT NULL REFERENCES category (id),
    id_equipo uuid NOT NULL REFERENCES team (id),
    orden integer NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT grupos_equipo_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT grupos_equipo_orden_check CHECK (orden > 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX grupos_equipo_pkey ON grupos_equipo (id_grupo_equipo) TABLESPACE ts_away;
ALTER TABLE grupos_equipo ADD CONSTRAINT grupos_equipo_pkey PRIMARY KEY USING INDEX grupos_equipo_pkey;

CREATE UNIQUE INDEX grupos_equipo_categoria_equipo_key
    ON grupos_equipo (id_categoria, id_equipo)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX grupos_equipo_grupo_idx
    ON grupos_equipo (id_grupo)
    TABLESPACE ts_away;
