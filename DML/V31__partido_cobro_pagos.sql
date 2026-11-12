ALTER TABLE partidos_cobros ADD COLUMN valor_cobro_efectivo numeric(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE partidos_cobros ADD COLUMN valor_cobro_transferencia numeric(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE partidos_cobros ADD CONSTRAINT partidos_cobros_valor_cobro_efectivo_check
    CHECK (valor_cobro_efectivo >= 0);
ALTER TABLE partidos_cobros ADD CONSTRAINT partidos_cobros_valor_cobro_transferencia_check
    CHECK (valor_cobro_transferencia >= 0);
