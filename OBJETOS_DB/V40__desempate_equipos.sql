CREATE TABLE criterios_desempate_equipo (
    id_criterio_desempate_equipo uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    id_edicion uuid NOT NULL REFERENCES edition (id),
    codigo varchar(40) NOT NULL,
    orden integer NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT criterios_desempate_equipo_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT criterios_desempate_equipo_codigo_check CHECK (codigo IN ('FAIR_PLAY', 'GOLES_FAVOR', 'ENFRENTAMIENTO_DIRECTO')),
    CONSTRAINT criterios_desempate_equipo_orden_check CHECK (orden > 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX criterios_desempate_equipo_pkey
    ON criterios_desempate_equipo (id_criterio_desempate_equipo) TABLESPACE ts_away;
ALTER TABLE criterios_desempate_equipo
    ADD CONSTRAINT criterios_desempate_equipo_pkey PRIMARY KEY USING INDEX criterios_desempate_equipo_pkey;

CREATE UNIQUE INDEX criterios_desempate_equipo_codigo_key
    ON criterios_desempate_equipo (id_edicion, codigo) TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE UNIQUE INDEX criterios_desempate_equipo_orden_key
    ON criterios_desempate_equipo (id_edicion, orden) TABLESPACE ts_away
    WHERE estado = 'ACTIVO';

CREATE INDEX criterios_desempate_equipo_edicion_idx
    ON criterios_desempate_equipo (id_edicion) TABLESPACE ts_away;
