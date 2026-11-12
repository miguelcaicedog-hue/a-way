ALTER TABLE category ADD COLUMN estado varchar(20) NOT NULL DEFAULT 'ACTIVO';

ALTER TABLE category ADD CONSTRAINT category_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO'));
