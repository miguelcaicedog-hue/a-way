ALTER TABLE player DROP CONSTRAINT player_entry_numero_camisa_key;

CREATE UNIQUE INDEX player_entry_numero_camisa_activo_key
    ON player (team_entry_id, numero_camisa) TABLESPACE ts_away
    WHERE estado = 'ACTIVO';
