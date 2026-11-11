CREATE TABLE jornadas (
    id_jornada uuid NOT NULL,
    id_categoria uuid NOT NULL REFERENCES category (id),
    id_fase uuid NOT NULL REFERENCES fases (id_fase),
    numero integer NOT NULL,
    nombre varchar(80) NOT NULL,
    fecha date,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT jornadas_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT jornadas_numero_check CHECK (numero > 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX jornadas_pkey ON jornadas (id_jornada) TABLESPACE ts_away;
ALTER TABLE jornadas ADD CONSTRAINT jornadas_pkey PRIMARY KEY USING INDEX jornadas_pkey;

CREATE UNIQUE INDEX jornadas_categoria_numero_key
    ON jornadas (id_categoria, numero)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX jornadas_categoria_idx
    ON jornadas (id_categoria)
    TABLESPACE ts_away;

CREATE TABLE partidos (
    id_partido uuid NOT NULL,
    id_jornada uuid NOT NULL REFERENCES jornadas (id_jornada),
    id_categoria uuid NOT NULL REFERENCES category (id),
    id_fase uuid NOT NULL REFERENCES fases (id_fase),
    id_grupo uuid REFERENCES grupos (id_grupo),
    id_cruce uuid REFERENCES cruces (id_cruce),
    id_equipo_local uuid REFERENCES team (id),
    id_equipo_visita uuid REFERENCES team (id),
    posicion_local integer,
    id_grupo_local uuid REFERENCES grupos (id_grupo),
    posicion_visita integer,
    id_grupo_visita uuid REFERENCES grupos (id_grupo),
    orden integer NOT NULL,
    fecha date,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT partidos_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT partidos_orden_check CHECK (orden > 0),
    CONSTRAINT partidos_posicion_local_check CHECK (posicion_local IS NULL OR posicion_local > 0),
    CONSTRAINT partidos_posicion_visita_check CHECK (posicion_visita IS NULL OR posicion_visita > 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX partidos_pkey ON partidos (id_partido) TABLESPACE ts_away;
ALTER TABLE partidos ADD CONSTRAINT partidos_pkey PRIMARY KEY USING INDEX partidos_pkey;

CREATE INDEX partidos_jornada_idx
    ON partidos (id_jornada)
    TABLESPACE ts_away;

CREATE INDEX partidos_categoria_idx
    ON partidos (id_categoria)
    TABLESPACE ts_away;
