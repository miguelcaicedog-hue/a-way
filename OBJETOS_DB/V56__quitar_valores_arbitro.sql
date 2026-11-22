ALTER TABLE arbitros
    DROP CONSTRAINT arbitros_valor_pago_partido_check,
    DROP CONSTRAINT arbitros_valor_cobro_equipo_check,
    DROP CONSTRAINT arbitros_valor_pago_sin_pito_check,
    DROP COLUMN valor_pago_partido,
    DROP COLUMN valor_cobro_equipo,
    DROP COLUMN valor_pago_sin_pito;
