ALTER TABLE reglas_suspension
    ADD COLUMN estado_sancion varchar(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN sancion_jornadas integer NOT NULL DEFAULT 1,
    ADD CONSTRAINT reglas_suspension_estado_sancion_check CHECK (estado_sancion IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    ADD CONSTRAINT reglas_suspension_sancion_jornadas_check CHECK (sancion_jornadas >= 0);

ALTER TABLE suspensiones_jugador
    DROP CONSTRAINT suspensiones_jugador_motivo_check;

ALTER TABLE suspensiones_jugador
    ADD CONSTRAINT suspensiones_jugador_motivo_check
        CHECK (motivo IN ('ROJA_DIRECTA', 'AMARILLAS', 'ROJAS_ACUMULADAS', 'FALTA_MEDIA', 'FALTA_GRAVE', 'SANCION'));

ALTER TABLE suspensiones_exclusion
    DROP CONSTRAINT suspensiones_exclusion_motivo_check;

ALTER TABLE suspensiones_exclusion
    ADD CONSTRAINT suspensiones_exclusion_motivo_check
        CHECK (motivo IN ('ROJA_DIRECTA', 'AMARILLAS', 'ROJAS_ACUMULADAS', 'FALTA_MEDIA', 'FALTA_GRAVE', 'SANCION'));
