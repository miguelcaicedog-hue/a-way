ALTER TABLE partidos ADD COLUMN hora time;
ALTER TABLE partidos ADD COLUMN id_cancha uuid;

ALTER TABLE partidos
    ADD CONSTRAINT partidos_cancha_fk
    FOREIGN KEY (id_cancha) REFERENCES canchas (id_cancha);

CREATE INDEX partidos_id_cancha_idx
    ON partidos (id_cancha)
    TABLESPACE ts_away;
