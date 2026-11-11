ALTER TABLE category ADD COLUMN cantidad_fases integer NOT NULL DEFAULT 1;

ALTER TABLE category ADD CONSTRAINT category_cantidad_fases_check CHECK (cantidad_fases > 0);
