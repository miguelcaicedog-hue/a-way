CREATE TABLE politica_clave (
    id_politica_clave uuid NOT NULL,
    longitud_minima integer NOT NULL,
    requiere_letra boolean NOT NULL,
    requiere_numero boolean NOT NULL,
    requiere_especial boolean NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT politica_clave_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT politica_clave_longitud_check CHECK (longitud_minima > 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX politica_clave_pkey
    ON politica_clave (id_politica_clave) TABLESPACE ts_away;
ALTER TABLE politica_clave
    ADD CONSTRAINT politica_clave_pkey PRIMARY KEY USING INDEX politica_clave_pkey;

CREATE UNIQUE INDEX politica_clave_activa_key
    ON politica_clave (estado) TABLESPACE ts_away
    WHERE estado = 'ACTIVO';

INSERT INTO politica_clave (
    id_politica_clave,
    longitud_minima,
    requiere_letra,
    requiere_numero,
    requiere_especial,
    usuario_creacion,
    usuario_modificacion
) VALUES (
    '66666666-6666-6666-6666-666666666661',
    7,
    true,
    true,
    true,
    'master',
    'master'
);
