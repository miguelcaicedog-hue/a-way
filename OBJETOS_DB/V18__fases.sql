CREATE TABLE fases (
    id_fase uuid NOT NULL,
    id_categoria uuid NOT NULL REFERENCES category (id),
    nombre varchar(80) NOT NULL,
    orden integer NOT NULL,
    modalidad varchar(20) NOT NULL DEFAULT 'IDA',
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT fases_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT fases_orden_check CHECK (orden > 0),
    CONSTRAINT fases_modalidad_check CHECK (modalidad IN ('IDA', 'IDA_Y_VUELTA'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX fases_pkey ON fases (id_fase) TABLESPACE ts_away;
ALTER TABLE fases ADD CONSTRAINT fases_pkey PRIMARY KEY USING INDEX fases_pkey;

CREATE UNIQUE INDEX fases_categoria_orden_key
    ON fases (id_categoria, orden)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX fases_categoria_idx
    ON fases (id_categoria)
    TABLESPACE ts_away;

CREATE TABLE cruces (
    id_cruce uuid NOT NULL,
    id_fase uuid NOT NULL REFERENCES fases (id_fase),
    id_categoria uuid NOT NULL REFERENCES category (id),
    orden integer NOT NULL,
    posicion_local integer,
    id_grupo_local uuid REFERENCES grupos (id_grupo),
    posicion_visita integer,
    id_grupo_visita uuid REFERENCES grupos (id_grupo),
    id_cruce_origen_local uuid,
    id_cruce_origen_visita uuid,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT cruces_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT cruces_orden_check CHECK (orden > 0),
    CONSTRAINT cruces_posicion_local_check CHECK (posicion_local IS NULL OR posicion_local > 0),
    CONSTRAINT cruces_posicion_visita_check CHECK (posicion_visita IS NULL OR posicion_visita > 0),
    CONSTRAINT cruces_local_par_check CHECK ((posicion_local IS NULL) = (id_grupo_local IS NULL)),
    CONSTRAINT cruces_visita_par_check CHECK ((posicion_visita IS NULL) = (id_grupo_visita IS NULL))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX cruces_pkey ON cruces (id_cruce) TABLESPACE ts_away;
ALTER TABLE cruces ADD CONSTRAINT cruces_pkey PRIMARY KEY USING INDEX cruces_pkey;

ALTER TABLE cruces
    ADD CONSTRAINT cruces_origen_local_fk FOREIGN KEY (id_cruce_origen_local) REFERENCES cruces (id_cruce);
ALTER TABLE cruces
    ADD CONSTRAINT cruces_origen_visita_fk FOREIGN KEY (id_cruce_origen_visita) REFERENCES cruces (id_cruce);

CREATE UNIQUE INDEX cruces_fase_orden_key
    ON cruces (id_fase, orden)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE UNIQUE INDEX cruces_posicion_local_key
    ON cruces (id_categoria, id_grupo_local, posicion_local)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO' AND id_grupo_local IS NOT NULL;

CREATE UNIQUE INDEX cruces_posicion_visita_key
    ON cruces (id_categoria, id_grupo_visita, posicion_visita)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO' AND id_grupo_visita IS NOT NULL;

CREATE INDEX cruces_categoria_idx
    ON cruces (id_categoria)
    TABLESPACE ts_away;

CREATE INDEX cruces_fase_idx
    ON cruces (id_fase)
    TABLESPACE ts_away;
