ALTER TABLE app_user
    ADD COLUMN estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN foto bytea,
    ADD COLUMN foto_content_type varchar(80),
    ADD COLUMN fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN usuario_creacion varchar(80) NOT NULL DEFAULT 'master',
    ADD COLUMN usuario_modificacion varchar(80) NOT NULL DEFAULT 'master';

ALTER TABLE app_user
    ADD CONSTRAINT app_user_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO'));

UPDATE app_user AS destino
SET usuario_creacion = origen.username,
    usuario_modificacion = origen.username
FROM (
    SELECT username
    FROM app_user
    WHERE master IS TRUE
    ORDER BY username
    LIMIT 1
) AS origen;
