ALTER TABLE player
    ADD COLUMN estado varchar(20) NOT NULL DEFAULT 'ACTIVO';

ALTER TABLE player
    ADD CONSTRAINT player_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'));
