CREATE OR REPLACE FUNCTION enforce_max_players() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    current_count integer;
    max_allowed integer;
BEGIN
    IF NEW.estado IS DISTINCT FROM 'ACTIVO' THEN
        RETURN NEW;
    END IF;

    SELECT c.max_players INTO max_allowed
    FROM team_entry te
    JOIN category c ON c.id = te.category_id
    WHERE te.id = NEW.team_entry_id;

    SELECT COUNT(*) INTO current_count
    FROM player
    WHERE team_entry_id = NEW.team_entry_id
      AND estado = 'ACTIVO'
      AND id IS DISTINCT FROM NEW.id;

    IF current_count >= max_allowed THEN
        RAISE EXCEPTION 'roster exceeds max_players';
    END IF;
    RETURN NEW;
END;
$$;
