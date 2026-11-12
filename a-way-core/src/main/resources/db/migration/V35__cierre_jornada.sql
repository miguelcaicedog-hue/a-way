ALTER TABLE jornadas
    ADD COLUMN estado_cierre varchar(20) NOT NULL DEFAULT 'ABIERTA',
    ADD CONSTRAINT jornadas_estado_cierre_check CHECK (estado_cierre IN ('ABIERTA', 'CERRADA'));

CREATE TABLE suspensiones_jugador (
    id_suspension_jugador uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    id_categoria uuid NOT NULL REFERENCES category (id),
    id_jugador uuid NOT NULL REFERENCES player (id),
    id_jornada_origen uuid NOT NULL REFERENCES jornadas (id_jornada),
    id_jornada_inicio uuid NOT NULL REFERENCES jornadas (id_jornada),
    jornadas integer NOT NULL,
    motivo varchar(30) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT suspensiones_jugador_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT suspensiones_jugador_jornadas_check CHECK (jornadas > 0),
    CONSTRAINT suspensiones_jugador_motivo_check CHECK (motivo IN ('ROJA_DIRECTA', 'AMARILLAS', 'ROJAS_ACUMULADAS'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX suspensiones_jugador_pkey ON suspensiones_jugador (id_suspension_jugador) TABLESPACE ts_away;
ALTER TABLE suspensiones_jugador ADD CONSTRAINT suspensiones_jugador_pkey PRIMARY KEY USING INDEX suspensiones_jugador_pkey;

CREATE INDEX suspensiones_jugador_categoria_idx
    ON suspensiones_jugador (id_categoria)
    TABLESPACE ts_away;
