CREATE TABLE tarjetas (
    id_tarjeta uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    descripcion varchar(160) NOT NULL,
    color varchar(7) NOT NULL,
    costo numeric(12, 2) NOT NULL,
    cantidad_jornadas integer NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT tarjetas_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT tarjetas_color_check CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT tarjetas_costo_check CHECK (costo >= 0),
    CONSTRAINT tarjetas_jornadas_check CHECK (cantidad_jornadas >= 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX tarjetas_pkey ON tarjetas (id_tarjeta) TABLESPACE ts_away;
ALTER TABLE tarjetas ADD CONSTRAINT tarjetas_pkey PRIMARY KEY USING INDEX tarjetas_pkey;

CREATE UNIQUE INDEX tarjetas_liga_descripcion_key
    ON tarjetas (id_liga, lower(descripcion))
    TABLESPACE ts_away
    WHERE estado = 'ACTIVO';

CREATE INDEX tarjetas_liga_idx
    ON tarjetas (id_liga)
    TABLESPACE ts_away;
