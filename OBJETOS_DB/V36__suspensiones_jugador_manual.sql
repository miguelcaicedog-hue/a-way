ALTER TABLE suspensiones_jugador
    ADD COLUMN origen varchar(20) NOT NULL DEFAULT 'CIERRE',
    ADD CONSTRAINT suspensiones_jugador_origen_check CHECK (origen IN ('CIERRE', 'MANUAL'));

ALTER TABLE suspensiones_jugador
    DROP CONSTRAINT suspensiones_jugador_motivo_check;

ALTER TABLE suspensiones_jugador
    ADD CONSTRAINT suspensiones_jugador_motivo_check
        CHECK (motivo IN ('ROJA_DIRECTA', 'AMARILLAS', 'ROJAS_ACUMULADAS', 'FALTA_MEDIA', 'FALTA_GRAVE'));

CREATE TABLE suspensiones_exclusion (
    id_suspension_exclusion uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    id_categoria uuid NOT NULL REFERENCES category (id),
    id_jugador uuid NOT NULL REFERENCES player (id),
    id_jornada_origen uuid NOT NULL REFERENCES jornadas (id_jornada),
    motivo varchar(30) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT suspensiones_exclusion_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT suspensiones_exclusion_motivo_check
        CHECK (motivo IN ('ROJA_DIRECTA', 'AMARILLAS', 'ROJAS_ACUMULADAS', 'FALTA_MEDIA', 'FALTA_GRAVE'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX suspensiones_exclusion_pkey
    ON suspensiones_exclusion (id_suspension_exclusion) TABLESPACE ts_away;
ALTER TABLE suspensiones_exclusion
    ADD CONSTRAINT suspensiones_exclusion_pkey PRIMARY KEY USING INDEX suspensiones_exclusion_pkey;

CREATE INDEX suspensiones_exclusion_categoria_idx
    ON suspensiones_exclusion (id_categoria) TABLESPACE ts_away;

CREATE UNIQUE INDEX suspensiones_exclusion_causa_idx
    ON suspensiones_exclusion (id_jugador, id_jornada_origen, motivo) TABLESPACE ts_away
    WHERE estado = 'ACTIVO';
