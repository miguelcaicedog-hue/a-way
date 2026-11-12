CREATE TABLE sanciones_jugador (
    id_sancion_jugador uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    descripcion varchar(160) NOT NULL,
    costo numeric(12, 2) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT sanciones_jugador_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT sanciones_jugador_costo_check CHECK (costo >= 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX sanciones_jugador_pkey ON sanciones_jugador (id_sancion_jugador) TABLESPACE ts_away;
ALTER TABLE sanciones_jugador ADD CONSTRAINT sanciones_jugador_pkey PRIMARY KEY USING INDEX sanciones_jugador_pkey;

CREATE UNIQUE INDEX sanciones_jugador_liga_descripcion_key
    ON sanciones_jugador (id_liga, lower(descripcion))
    TABLESPACE ts_away
    WHERE estado = 'ACTIVO';

CREATE INDEX sanciones_jugador_liga_idx
    ON sanciones_jugador (id_liga)
    TABLESPACE ts_away;
