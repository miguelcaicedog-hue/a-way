DELETE FROM suspensiones_exclusion
WHERE motivo = 'FALTA_MEDIA';

DELETE FROM suspensiones_jugador
WHERE motivo = 'FALTA_MEDIA';

ALTER TABLE suspensiones_jugador
    DROP CONSTRAINT suspensiones_jugador_motivo_check;

ALTER TABLE suspensiones_jugador
    ADD CONSTRAINT suspensiones_jugador_motivo_check
        CHECK (motivo IN ('ROJA_DIRECTA', 'AMARILLAS', 'ROJAS_ACUMULADAS', 'FALTA_GRAVE', 'SANCION'));

ALTER TABLE suspensiones_exclusion
    DROP CONSTRAINT suspensiones_exclusion_motivo_check;

ALTER TABLE suspensiones_exclusion
    ADD CONSTRAINT suspensiones_exclusion_motivo_check
        CHECK (motivo IN ('ROJA_DIRECTA', 'AMARILLAS', 'ROJAS_ACUMULADAS', 'FALTA_GRAVE', 'SANCION'));

ALTER TABLE reglas_suspension
    DROP CONSTRAINT reglas_suspension_falta_media_check,
    DROP CONSTRAINT reglas_suspension_estado_falta_media_check,
    DROP COLUMN falta_media_jornadas,
    DROP COLUMN estado_falta_media;
