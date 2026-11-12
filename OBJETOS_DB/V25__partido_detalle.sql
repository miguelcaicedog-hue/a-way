CREATE TABLE partidos_arbitros (
    id_partido_arbitro uuid NOT NULL,
    id_partido uuid NOT NULL REFERENCES partidos (id_partido),
    id_arbitro uuid NOT NULL REFERENCES arbitros (id_arbitro),
    pito boolean NOT NULL DEFAULT true,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT partidos_arbitros_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX partidos_arbitros_pkey ON partidos_arbitros (id_partido_arbitro) TABLESPACE ts_away;
ALTER TABLE partidos_arbitros ADD CONSTRAINT partidos_arbitros_pkey PRIMARY KEY USING INDEX partidos_arbitros_pkey;

CREATE UNIQUE INDEX partidos_arbitros_partido_arbitro_key
    ON partidos_arbitros (id_partido, id_arbitro)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX partidos_arbitros_partido_idx
    ON partidos_arbitros (id_partido)
    TABLESPACE ts_away;

CREATE TABLE goles_jugadores (
    id_gol_jugador uuid NOT NULL,
    id_partido uuid NOT NULL REFERENCES partidos (id_partido),
    id_jugador uuid NOT NULL REFERENCES player (id),
    cantidad integer NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT goles_jugadores_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT goles_jugadores_cantidad_check CHECK (cantidad > 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX goles_jugadores_pkey ON goles_jugadores (id_gol_jugador) TABLESPACE ts_away;
ALTER TABLE goles_jugadores ADD CONSTRAINT goles_jugadores_pkey PRIMARY KEY USING INDEX goles_jugadores_pkey;

CREATE UNIQUE INDEX goles_jugadores_partido_jugador_key
    ON goles_jugadores (id_partido, id_jugador)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX goles_jugadores_partido_idx
    ON goles_jugadores (id_partido)
    TABLESPACE ts_away;
