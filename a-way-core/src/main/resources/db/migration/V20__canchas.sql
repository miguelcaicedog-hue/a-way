CREATE TABLE canchas (
    id_cancha uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    descripcion varchar(160) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT canchas_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX canchas_pkey ON canchas (id_cancha) TABLESPACE ts_away;
ALTER TABLE canchas ADD CONSTRAINT canchas_pkey PRIMARY KEY USING INDEX canchas_pkey;

CREATE UNIQUE INDEX canchas_liga_descripcion_key
    ON canchas (id_liga, lower(descripcion))
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX canchas_liga_idx
    ON canchas (id_liga)
    TABLESPACE ts_away;
