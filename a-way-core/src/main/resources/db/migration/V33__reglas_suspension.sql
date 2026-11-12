CREATE TABLE reglas_suspension (
    id_regla_suspension uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    roja_jornadas integer NOT NULL,
    amarillas_acumuladas integer NOT NULL,
    amarillas_jornadas integer NOT NULL,
    rojas_acumuladas integer NOT NULL,
    rojas_jornadas integer NOT NULL,
    falta_media_jornadas integer NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT reglas_suspension_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT reglas_suspension_roja_check CHECK (roja_jornadas >= 0),
    CONSTRAINT reglas_suspension_amarillas_umbral_check CHECK (amarillas_acumuladas > 0),
    CONSTRAINT reglas_suspension_amarillas_jornadas_check CHECK (amarillas_jornadas >= 0),
    CONSTRAINT reglas_suspension_rojas_umbral_check CHECK (rojas_acumuladas > 0),
    CONSTRAINT reglas_suspension_rojas_jornadas_check CHECK (rojas_jornadas >= 0),
    CONSTRAINT reglas_suspension_falta_media_check CHECK (falta_media_jornadas >= 0)
) TABLESPACE ts_away;

CREATE UNIQUE INDEX reglas_suspension_pkey ON reglas_suspension (id_regla_suspension) TABLESPACE ts_away;
ALTER TABLE reglas_suspension ADD CONSTRAINT reglas_suspension_pkey PRIMARY KEY USING INDEX reglas_suspension_pkey;

CREATE UNIQUE INDEX reglas_suspension_liga_key
    ON reglas_suspension (id_liga)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

INSERT INTO reglas_suspension (
    id_regla_suspension, id_liga, roja_jornadas, amarillas_acumuladas, amarillas_jornadas,
    rojas_acumuladas, rojas_jornadas, falta_media_jornadas,
    usuario_creacion, usuario_modificacion, estado
)
SELECT gen_random_uuid(), id, 1, 3, 1, 2, 2, 3, 'sistema', 'sistema', 'ACTIVO'
FROM league;
