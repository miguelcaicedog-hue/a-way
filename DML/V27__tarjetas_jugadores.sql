CREATE TABLE tarjetas_jugadores (
    id_tarjeta_jugador uuid NOT NULL,
    id_partido uuid NOT NULL REFERENCES partidos (id_partido),
    id_jornada uuid NOT NULL REFERENCES jornadas (id_jornada),
    fecha date,
    id_jugador uuid NOT NULL REFERENCES player (id),
    amarillas integer,
    rojas integer,
    observacion varchar(300),
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT tarjetas_jugadores_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT tarjetas_jugadores_amarillas_check CHECK (amarillas IS NULL OR amarillas IN (1, 2)),
    CONSTRAINT tarjetas_jugadores_rojas_check CHECK (rojas IS NULL OR rojas = 1),
    CONSTRAINT tarjetas_jugadores_contenido_check CHECK (
        amarillas IS NOT NULL
        OR rojas IS NOT NULL
        OR (observacion IS NOT NULL AND length(btrim(observacion)) > 0)
    )
) TABLESPACE ts_away;

CREATE UNIQUE INDEX tarjetas_jugadores_pkey ON tarjetas_jugadores (id_tarjeta_jugador) TABLESPACE ts_away;
ALTER TABLE tarjetas_jugadores ADD CONSTRAINT tarjetas_jugadores_pkey PRIMARY KEY USING INDEX tarjetas_jugadores_pkey;

CREATE UNIQUE INDEX tarjetas_jugadores_partido_jugador_key
    ON tarjetas_jugadores (id_partido, id_jugador)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX tarjetas_jugadores_partido_idx
    ON tarjetas_jugadores (id_partido)
    TABLESPACE ts_away;
