CREATE TABLE arbitros (
    id_arbitro uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    cedula varchar(20),
    nombre varchar(160) NOT NULL,
    telefono varchar(30),
    foto bytea,
    foto_content_type varchar(80),
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT arbitros_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX arbitros_pkey ON arbitros (id_arbitro) TABLESPACE ts_away;
ALTER TABLE arbitros ADD CONSTRAINT arbitros_pkey PRIMARY KEY USING INDEX arbitros_pkey;

CREATE UNIQUE INDEX arbitros_liga_cedula_key
    ON arbitros (id_liga, cedula)
    TABLESPACE ts_away
    WHERE cedula IS NOT NULL AND estado <> 'ELIMINADO';

CREATE INDEX arbitros_liga_idx
    ON arbitros (id_liga)
    TABLESPACE ts_away;
