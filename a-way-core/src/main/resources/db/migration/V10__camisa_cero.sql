ALTER TABLE player DROP CONSTRAINT player_numero_camisa_check;
ALTER TABLE player ADD CONSTRAINT player_numero_camisa_check CHECK (numero_camisa >= 0);
