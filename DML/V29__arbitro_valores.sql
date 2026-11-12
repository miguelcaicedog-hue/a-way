ALTER TABLE arbitros ADD COLUMN valor_pago_partido numeric(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE arbitros ADD COLUMN valor_cobro_equipo numeric(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE arbitros ADD COLUMN valor_pago_sin_pito numeric(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE arbitros ADD CONSTRAINT arbitros_valor_pago_partido_check CHECK (valor_pago_partido >= 0);
ALTER TABLE arbitros ADD CONSTRAINT arbitros_valor_cobro_equipo_check CHECK (valor_cobro_equipo >= 0);
ALTER TABLE arbitros ADD CONSTRAINT arbitros_valor_pago_sin_pito_check CHECK (valor_pago_sin_pito >= 0);
