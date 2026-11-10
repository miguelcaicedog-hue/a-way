ALTER TABLE player ADD COLUMN cedula varchar(30);

UPDATE player SET cedula = shirt_number::text;

ALTER TABLE player ALTER COLUMN cedula SET NOT NULL;

ALTER TABLE player DROP CONSTRAINT player_shirt_number_check;
ALTER TABLE player DROP CONSTRAINT player_entry_number_key;

ALTER TABLE player DROP COLUMN shirt_number;

ALTER TABLE player ADD CONSTRAINT player_entry_cedula_key UNIQUE (team_entry_id, cedula);
