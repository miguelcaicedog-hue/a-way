ALTER TABLE team ADD COLUMN fecha_creacion timestamptz;
ALTER TABLE team ADD COLUMN fecha_modificacion timestamptz;
ALTER TABLE team ADD COLUMN usuario_creacion varchar(80);
ALTER TABLE team ADD COLUMN usuario_modificacion varchar(80);
ALTER TABLE team ADD COLUMN estado varchar(20);

UPDATE team
SET fecha_creacion = CURRENT_TIMESTAMP,
    fecha_modificacion = CURRENT_TIMESTAMP,
    usuario_creacion = (SELECT username FROM app_user WHERE master IS TRUE ORDER BY username LIMIT 1),
    usuario_modificacion = (SELECT username FROM app_user WHERE master IS TRUE ORDER BY username LIMIT 1),
    estado = 'ACTIVO'
WHERE estado IS NULL;

ALTER TABLE team ALTER COLUMN fecha_creacion SET NOT NULL;
ALTER TABLE team ALTER COLUMN fecha_modificacion SET NOT NULL;
ALTER TABLE team ALTER COLUMN usuario_creacion SET NOT NULL;
ALTER TABLE team ALTER COLUMN usuario_modificacion SET NOT NULL;
ALTER TABLE team ALTER COLUMN estado SET NOT NULL;
ALTER TABLE team ALTER COLUMN estado SET DEFAULT 'ACTIVO';

ALTER TABLE team ADD CONSTRAINT team_estado_check
    CHECK (estado IN ('ACTIVO', 'BLOQUEADO', 'INACTIVO', 'ELIMINADO'));

ALTER TABLE team DROP CONSTRAINT team_league_name_key;

CREATE UNIQUE INDEX team_league_name_key
    ON team (league_id, name)
    TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';
