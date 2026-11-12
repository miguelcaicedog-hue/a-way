CREATE TABLE partidos_cobros (
    id_partido_cobro uuid NOT NULL,
    id_partido uuid NOT NULL REFERENCES partidos (id_partido),
    id_jornada uuid NOT NULL REFERENCES jornadas (id_jornada),
    fecha date,
    id_equipo uuid NOT NULL REFERENCES team (id),
    observacion varchar(300),
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT partidos_cobros_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO'))
) TABLESPACE ts_away;

CREATE UNIQUE INDEX partidos_cobros_pkey ON partidos_cobros (id_partido_cobro) TABLESPACE ts_away;
ALTER TABLE partidos_cobros ADD CONSTRAINT partidos_cobros_pkey PRIMARY KEY USING INDEX partidos_cobros_pkey;

CREATE UNIQUE INDEX partidos_cobros_partido_equipo_key
    ON partidos_cobros (id_partido, id_equipo)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX partidos_cobros_partido_idx ON partidos_cobros (id_partido) TABLESPACE ts_away;

CREATE TABLE partidos_cobros_detalle (
    id_partido_cobro_detalle uuid NOT NULL,
    id_partido_cobro uuid NOT NULL REFERENCES partidos_cobros (id_partido_cobro),
    id_partido uuid NOT NULL REFERENCES partidos (id_partido),
    id_jornada uuid NOT NULL REFERENCES jornadas (id_jornada),
    fecha date,
    id_equipo uuid NOT NULL REFERENCES team (id),
    tipo varchar(20) NOT NULL,
    descripcion varchar(200) NOT NULL,
    costo numeric(12, 2) NOT NULL,
    id_arbitro uuid REFERENCES arbitros (id_arbitro),
    id_tarjeta_jugador uuid REFERENCES tarjetas_jugadores (id_tarjeta_jugador),
    id_partido_sancion_detalle uuid REFERENCES partidos_sanciones_detalle (id_partido_sancion_detalle),
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT partidos_cobros_detalle_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT partidos_cobros_detalle_tipo_check CHECK (tipo IN ('ARBITRAJE', 'TARJETA', 'SANCION')),
    CONSTRAINT partidos_cobros_detalle_costo_check CHECK (costo >= 0),
    CONSTRAINT partidos_cobros_detalle_origen_check CHECK (
        (tipo = 'ARBITRAJE' AND id_arbitro IS NOT NULL AND id_tarjeta_jugador IS NULL AND id_partido_sancion_detalle IS NULL)
        OR (tipo = 'TARJETA' AND id_tarjeta_jugador IS NOT NULL AND id_arbitro IS NULL AND id_partido_sancion_detalle IS NULL)
        OR (tipo = 'SANCION' AND id_partido_sancion_detalle IS NOT NULL AND id_arbitro IS NULL AND id_tarjeta_jugador IS NULL)
    )
) TABLESPACE ts_away;

CREATE UNIQUE INDEX partidos_cobros_detalle_pkey
    ON partidos_cobros_detalle (id_partido_cobro_detalle) TABLESPACE ts_away;
ALTER TABLE partidos_cobros_detalle
    ADD CONSTRAINT partidos_cobros_detalle_pkey PRIMARY KEY USING INDEX partidos_cobros_detalle_pkey;

CREATE UNIQUE INDEX partidos_cobros_detalle_arbitro_key
    ON partidos_cobros_detalle (id_partido_cobro, id_arbitro)
    TABLESPACE ts_away
    WHERE tipo = 'ARBITRAJE' AND estado <> 'ELIMINADO';

CREATE UNIQUE INDEX partidos_cobros_detalle_tarjeta_key
    ON partidos_cobros_detalle (id_tarjeta_jugador)
    TABLESPACE ts_away
    WHERE tipo = 'TARJETA' AND estado <> 'ELIMINADO';

CREATE UNIQUE INDEX partidos_cobros_detalle_sancion_key
    ON partidos_cobros_detalle (id_partido_sancion_detalle)
    TABLESPACE ts_away
    WHERE tipo = 'SANCION' AND estado <> 'ELIMINADO';

CREATE INDEX partidos_cobros_detalle_cobro_idx
    ON partidos_cobros_detalle (id_partido_cobro) TABLESPACE ts_away;
