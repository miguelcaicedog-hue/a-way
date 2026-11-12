ALTER TABLE partidos ADD COLUMN goles_local integer;
ALTER TABLE partidos ADD COLUMN goles_visita integer;

ALTER TABLE partidos
    ADD CONSTRAINT partidos_goles_local_check CHECK (goles_local IS NULL OR goles_local >= 0);

ALTER TABLE partidos
    ADD CONSTRAINT partidos_goles_visita_check CHECK (goles_visita IS NULL OR goles_visita >= 0);
