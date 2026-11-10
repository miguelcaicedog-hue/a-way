ALTER TABLE player ADD COLUMN numero_camisa integer;

UPDATE player AS jugador
SET numero_camisa = orden.n
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY team_entry_id ORDER BY cedula) AS n
    FROM player
) AS orden
WHERE jugador.id = orden.id
  AND jugador.numero_camisa IS NULL;

ALTER TABLE player ALTER COLUMN numero_camisa SET NOT NULL;

ALTER TABLE player ADD CONSTRAINT player_numero_camisa_check CHECK (numero_camisa > 0);

ALTER TABLE player ADD CONSTRAINT player_entry_numero_camisa_key UNIQUE (team_entry_id, numero_camisa);
