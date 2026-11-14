ALTER TABLE reglas_suspension
    ADD COLUMN estado_roja varchar(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN estado_amarillas varchar(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN estado_rojas varchar(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN estado_falta_media varchar(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN estado_falta_grave varchar(20) NOT NULL DEFAULT 'ACTIVO',
    ADD CONSTRAINT reglas_suspension_estado_roja_check CHECK (estado_roja IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    ADD CONSTRAINT reglas_suspension_estado_amarillas_check CHECK (estado_amarillas IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    ADD CONSTRAINT reglas_suspension_estado_rojas_check CHECK (estado_rojas IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    ADD CONSTRAINT reglas_suspension_estado_falta_media_check CHECK (estado_falta_media IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    ADD CONSTRAINT reglas_suspension_estado_falta_grave_check CHECK (estado_falta_grave IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'));
