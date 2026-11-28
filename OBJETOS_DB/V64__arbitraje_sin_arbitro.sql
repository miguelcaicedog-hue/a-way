ALTER TABLE partidos_cobros_detalle
    DROP CONSTRAINT partidos_cobros_detalle_origen_check;

ALTER TABLE partidos_cobros_detalle
    ADD CONSTRAINT partidos_cobros_detalle_origen_check CHECK (
        (tipo = 'ARBITRAJE' AND id_tarjeta_jugador IS NULL AND id_partido_sancion_detalle IS NULL)
        OR (tipo = 'TARJETA' AND id_tarjeta_jugador IS NOT NULL AND id_arbitro IS NULL AND id_partido_sancion_detalle IS NULL)
        OR (tipo = 'SANCION' AND id_partido_sancion_detalle IS NOT NULL AND id_arbitro IS NULL AND id_tarjeta_jugador IS NULL)
    );

CREATE UNIQUE INDEX partidos_cobros_detalle_arbitraje_sin_arbitro_key
    ON partidos_cobros_detalle (id_partido_cobro)
    TABLESPACE ts_away
    WHERE tipo = 'ARBITRAJE' AND id_arbitro IS NULL AND estado <> 'ELIMINADO';
