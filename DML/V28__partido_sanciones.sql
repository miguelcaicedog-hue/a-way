CREATE TABLE partidos_sanciones (
    id_partido_sancion uuid NOT NULL,
    id_partido uuid NOT NULL REFERENCES partidos (id_partido),
    id_jornada uuid NOT NULL REFERENCES jornadas (id_jornada),
    fecha date,
    id_jugador uuid NOT NULL REFERENCES player (id),
    observacion varchar(300),
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT partidos_sanciones_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX partidos_sanciones_pkey ON partidos_sanciones (id_partido_sancion) TABLESPACE ts_away;
ALTER TABLE partidos_sanciones ADD CONSTRAINT partidos_sanciones_pkey PRIMARY KEY USING INDEX partidos_sanciones_pkey;

CREATE UNIQUE INDEX partidos_sanciones_partido_jugador_key
    ON partidos_sanciones (id_partido, id_jugador)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX partidos_sanciones_partido_idx
    ON partidos_sanciones (id_partido)
    TABLESPACE ts_away;

CREATE TABLE partidos_sanciones_detalle (
    id_partido_sancion_detalle uuid NOT NULL,
    id_partido_sancion uuid NOT NULL REFERENCES partidos_sanciones (id_partido_sancion),
    id_sancion_jugador uuid NOT NULL REFERENCES sanciones_jugador (id_sancion_jugador),
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT partidos_sanciones_detalle_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX partidos_sanciones_detalle_pkey
    ON partidos_sanciones_detalle (id_partido_sancion_detalle) TABLESPACE ts_away;
ALTER TABLE partidos_sanciones_detalle
    ADD CONSTRAINT partidos_sanciones_detalle_pkey PRIMARY KEY USING INDEX partidos_sanciones_detalle_pkey;

CREATE UNIQUE INDEX partidos_sanciones_detalle_sancion_key
    ON partidos_sanciones_detalle (id_partido_sancion, id_sancion_jugador)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX partidos_sanciones_detalle_sancion_idx
    ON partidos_sanciones_detalle (id_partido_sancion)
    TABLESPACE ts_away;
