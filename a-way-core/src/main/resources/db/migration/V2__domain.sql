CREATE TABLE app_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username varchar(80) NOT NULL,
    password_hash varchar(255),
    google_subject varchar(255),
    email varchar(255),
    display_name varchar(160) NOT NULL,
    master boolean NOT NULL DEFAULT false,
    CONSTRAINT app_user_username_key UNIQUE (username),
    CONSTRAINT app_user_google_subject_key UNIQUE (google_subject),
    CONSTRAINT app_user_login_present CHECK (password_hash IS NOT NULL OR google_subject IS NOT NULL)
);

CREATE TABLE league (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(160) NOT NULL,
    valid_from date NOT NULL,
    valid_until date NOT NULL,
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT league_validity_order CHECK (valid_until >= valid_from)
);

CREATE TABLE league_membership (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_user (id),
    league_id uuid NOT NULL REFERENCES league (id),
    role varchar(20) NOT NULL,
    CONSTRAINT league_membership_role_check CHECK (role IN ('ADMINISTRATIVO', 'ASISTENTE')),
    CONSTRAINT league_membership_user_league_key UNIQUE (user_id, league_id)
);

CREATE TABLE suspension_rule (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id uuid NOT NULL REFERENCES league (id),
    card_color varchar(10) NOT NULL,
    threshold integer NOT NULL,
    suspension_matches integer NOT NULL,
    scope varchar(20) NOT NULL,
    CONSTRAINT suspension_rule_color_check CHECK (card_color IN ('YELLOW', 'RED')),
    CONSTRAINT suspension_rule_scope_check CHECK (scope IN ('DIRECT', 'ACCUMULATED')),
    CONSTRAINT suspension_rule_threshold_check CHECK (threshold > 0),
    CONSTRAINT suspension_rule_matches_check CHECK (suspension_matches > 0),
    CONSTRAINT suspension_rule_direct_threshold CHECK (scope <> 'DIRECT' OR threshold = 1)
);

CREATE TABLE league_settings (
    league_id uuid PRIMARY KEY REFERENCES league (id),
    suspension_timing varchar(30) NOT NULL,
    fair_play_yellow_points integer NOT NULL,
    fair_play_red_points integer NOT NULL,
    CONSTRAINT league_settings_timing_check CHECK (suspension_timing IN ('NEXT_MATCHDAY', 'FIRST_MATCHDAY_OF_DAY')),
    CONSTRAINT league_settings_points_check CHECK (fair_play_yellow_points >= 0 AND fair_play_red_points >= 0)
);

CREATE TABLE edition (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id uuid NOT NULL REFERENCES league (id),
    name varchar(160) NOT NULL,
    starts_on date NOT NULL,
    ends_on date NOT NULL,
    CONSTRAINT edition_dates_order CHECK (ends_on >= starts_on),
    CONSTRAINT edition_league_name_key UNIQUE (league_id, name)
);

CREATE TABLE category (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    edition_id uuid NOT NULL REFERENCES edition (id),
    name varchar(160) NOT NULL,
    max_players integer NOT NULL,
    CONSTRAINT category_max_players_check CHECK (max_players > 0),
    CONSTRAINT category_edition_name_key UNIQUE (edition_id, name)
);

CREATE TABLE tiebreaker (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES category (id),
    sort_order integer NOT NULL,
    criterion varchar(30) NOT NULL,
    CONSTRAINT tiebreaker_order_check CHECK (sort_order > 0),
    CONSTRAINT tiebreaker_criterion_check CHECK (criterion IN ('GOAL_DIFFERENCE', 'GOALS_FOR', 'FAIR_PLAY')),
    CONSTRAINT tiebreaker_category_order_key UNIQUE (category_id, sort_order),
    CONSTRAINT tiebreaker_category_criterion_key UNIQUE (category_id, criterion)
);

CREATE TABLE team (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id uuid NOT NULL REFERENCES league (id),
    name varchar(160) NOT NULL,
    CONSTRAINT team_league_name_key UNIQUE (league_id, name)
);

CREATE TABLE team_entry (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id uuid NOT NULL REFERENCES team (id),
    category_id uuid NOT NULL REFERENCES category (id),
    CONSTRAINT team_entry_team_category_key UNIQUE (team_id, category_id)
);

CREATE TABLE player (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_entry_id uuid NOT NULL REFERENCES team_entry (id),
    name varchar(160) NOT NULL,
    alias varchar(80),
    shirt_number integer NOT NULL,
    photo bytea,
    photo_content_type varchar(80),
    rotation_degrees smallint NOT NULL DEFAULT 0,
    CONSTRAINT player_shirt_number_check CHECK (shirt_number > 0),
    CONSTRAINT player_rotation_check CHECK (rotation_degrees IN (0, 90, 180, 270)),
    CONSTRAINT player_entry_number_key UNIQUE (team_entry_id, shirt_number)
);

CREATE TABLE enrollment_import (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id uuid NOT NULL REFERENCES league (id),
    category_id uuid REFERENCES category (id),
    external_response_id varchar(160) NOT NULL,
    payload jsonb NOT NULL,
    status varchar(20) NOT NULL,
    CONSTRAINT enrollment_import_status_check CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT enrollment_import_response_key UNIQUE (league_id, external_response_id)
);

CREATE TABLE competition_group (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES category (id),
    name varchar(80) NOT NULL,
    sort_order integer NOT NULL,
    CONSTRAINT competition_group_order_check CHECK (sort_order > 0),
    CONSTRAINT competition_group_category_name_key UNIQUE (category_id, name),
    CONSTRAINT competition_group_category_order_key UNIQUE (category_id, sort_order)
);

CREATE TABLE group_team (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id uuid NOT NULL REFERENCES competition_group (id),
    team_entry_id uuid NOT NULL REFERENCES team_entry (id),
    CONSTRAINT group_team_entry_key UNIQUE (team_entry_id),
    CONSTRAINT group_team_group_entry_key UNIQUE (group_id, team_entry_id)
);

CREATE TABLE grouping_draw (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES category (id),
    method varchar(20) NOT NULL,
    drawn_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT grouping_draw_method_check CHECK (method IN ('MANUAL', 'RANDOM', 'DRAW'))
);

CREATE TABLE phase (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES category (id),
    phase_number integer NOT NULL,
    kind varchar(20) NOT NULL,
    knockout_round varchar(20),
    home_and_away boolean NOT NULL DEFAULT false,
    CONSTRAINT phase_number_check CHECK (phase_number > 0),
    CONSTRAINT phase_kind_check CHECK (kind IN ('GROUP', 'KNOCKOUT')),
    CONSTRAINT phase_round_check CHECK (knockout_round IS NULL OR knockout_round IN ('ROUND_OF_16', 'QUARTER', 'SEMI', 'FINAL')),
    CONSTRAINT phase_kind_round_check CHECK (
        (kind = 'GROUP' AND knockout_round IS NULL)
        OR (kind = 'KNOCKOUT' AND knockout_round IS NOT NULL)
    ),
    CONSTRAINT phase_first_is_group CHECK (phase_number > 1 OR kind = 'GROUP'),
    CONSTRAINT phase_category_number_key UNIQUE (category_id, phase_number)
);

CREATE TABLE bracket_slot (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    phase_id uuid NOT NULL REFERENCES phase (id),
    home_group_id uuid NOT NULL REFERENCES competition_group (id),
    home_position integer NOT NULL,
    away_group_id uuid NOT NULL REFERENCES competition_group (id),
    away_position integer NOT NULL,
    CONSTRAINT bracket_slot_positions_check CHECK (home_position > 0 AND away_position > 0)
);

CREATE TABLE referee (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES category (id),
    name varchar(160) NOT NULL,
    CONSTRAINT referee_category_name_key UNIQUE (category_id, name)
);

CREATE TABLE field (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES category (id),
    name varchar(160) NOT NULL,
    CONSTRAINT field_category_name_key UNIQUE (category_id, name)
);

CREATE TABLE matchday (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES category (id),
    phase_id uuid NOT NULL REFERENCES phase (id),
    number integer NOT NULL,
    play_date date NOT NULL,
    kickoff_time time NOT NULL,
    CONSTRAINT matchday_number_check CHECK (number > 0),
    CONSTRAINT matchday_phase_number_key UNIQUE (phase_id, number)
);

CREATE TABLE match (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    matchday_id uuid NOT NULL REFERENCES matchday (id),
    group_id uuid REFERENCES competition_group (id),
    bracket_slot_id uuid REFERENCES bracket_slot (id),
    home_team_entry_id uuid NOT NULL REFERENCES team_entry (id),
    away_team_entry_id uuid NOT NULL REFERENCES team_entry (id),
    field_id uuid REFERENCES field (id),
    referee_id uuid REFERENCES referee (id),
    kickoff_time time,
    home_goals integer,
    away_goals integer,
    played boolean NOT NULL DEFAULT false,
    CONSTRAINT match_distinct_teams CHECK (home_team_entry_id <> away_team_entry_id),
    CONSTRAINT match_stage_check CHECK (
        (group_id IS NOT NULL AND bracket_slot_id IS NULL)
        OR (group_id IS NULL AND bracket_slot_id IS NOT NULL)
    ),
    CONSTRAINT match_goals_check CHECK (
        (home_goals IS NULL AND away_goals IS NULL)
        OR (home_goals >= 0 AND away_goals >= 0)
    )
);

CREATE TABLE field_matchday_referee (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    field_id uuid NOT NULL REFERENCES field (id),
    matchday_id uuid NOT NULL REFERENCES matchday (id),
    referee_id uuid NOT NULL REFERENCES referee (id),
    CONSTRAINT field_matchday_referee_key UNIQUE (field_id, matchday_id)
);

CREATE TABLE goal (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id uuid NOT NULL REFERENCES match (id),
    player_id uuid NOT NULL REFERENCES player (id),
    minute integer NOT NULL,
    CONSTRAINT goal_minute_check CHECK (minute >= 0)
);

CREATE TABLE player_card (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id uuid NOT NULL REFERENCES match (id),
    player_id uuid NOT NULL REFERENCES player (id),
    color varchar(10) NOT NULL,
    CONSTRAINT player_card_color_check CHECK (color IN ('YELLOW', 'RED'))
);

CREATE TABLE suspension (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id uuid NOT NULL REFERENCES player (id),
    suspension_rule_id uuid NOT NULL REFERENCES suspension_rule (id),
    trigger_card_id uuid NOT NULL REFERENCES player_card (id),
    matches_total integer NOT NULL,
    matches_served integer NOT NULL DEFAULT 0,
    starts_matchday_id uuid NOT NULL REFERENCES matchday (id),
    CONSTRAINT suspension_matches_check CHECK (matches_total > 0 AND matches_served >= 0 AND matches_served <= matches_total)
);

CREATE TABLE team_sanction (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_entry_id uuid NOT NULL REFERENCES team_entry (id),
    points_delta integer NOT NULL DEFAULT 0,
    goals_for_delta integer NOT NULL DEFAULT 0,
    goals_against_delta integer NOT NULL DEFAULT 0,
    reason varchar(240) NOT NULL
);

CREATE TABLE referee_payment (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_entry_id uuid NOT NULL REFERENCES team_entry (id),
    matchday_id uuid NOT NULL REFERENCES matchday (id),
    amount numeric(12, 2) NOT NULL,
    paid_on date,
    CONSTRAINT referee_payment_amount_check CHECK (amount >= 0),
    CONSTRAINT referee_payment_entry_matchday_key UNIQUE (team_entry_id, matchday_id)
);

CREATE TABLE card_fine (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    player_card_id uuid NOT NULL REFERENCES player_card (id),
    amount numeric(12, 2) NOT NULL,
    paid_on date,
    CONSTRAINT card_fine_amount_check CHECK (amount >= 0),
    CONSTRAINT card_fine_card_key UNIQUE (player_card_id)
);

CREATE INDEX league_membership_league_idx ON league_membership (league_id);
CREATE INDEX suspension_rule_league_idx ON suspension_rule (league_id);
CREATE INDEX edition_league_idx ON edition (league_id);
CREATE INDEX category_edition_idx ON category (edition_id);
CREATE INDEX team_league_idx ON team (league_id);
CREATE INDEX team_entry_category_idx ON team_entry (category_id);
CREATE INDEX player_team_entry_idx ON player (team_entry_id);
CREATE INDEX enrollment_import_category_idx ON enrollment_import (category_id);
CREATE INDEX matchday_category_date_idx ON matchday (category_id, play_date, number);
CREATE INDEX match_matchday_idx ON match (matchday_id);
CREATE INDEX goal_match_idx ON goal (match_id);
CREATE INDEX player_card_match_idx ON player_card (match_id);
CREATE INDEX suspension_player_idx ON suspension (player_id);

CREATE OR REPLACE FUNCTION team_entry_same_league() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM team t
        JOIN category c ON c.id = NEW.category_id
        JOIN edition e ON e.id = c.edition_id
        WHERE t.id = NEW.team_id
          AND t.league_id = e.league_id
    ) THEN
        RAISE EXCEPTION 'team and category belong to different leagues';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER team_entry_same_league_trg
    BEFORE INSERT OR UPDATE ON team_entry
    FOR EACH ROW EXECUTE PROCEDURE team_entry_same_league();

CREATE OR REPLACE FUNCTION enforce_max_players() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    current_count integer;
    max_allowed integer;
BEGIN
    SELECT c.max_players INTO max_allowed
    FROM team_entry te
    JOIN category c ON c.id = te.category_id
    WHERE te.id = NEW.team_entry_id;

    SELECT COUNT(*) INTO current_count
    FROM player
    WHERE team_entry_id = NEW.team_entry_id
      AND id IS DISTINCT FROM NEW.id;

    IF current_count >= max_allowed THEN
        RAISE EXCEPTION 'roster exceeds max_players';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER enforce_max_players_trg
    BEFORE INSERT OR UPDATE ON player
    FOR EACH ROW EXECUTE PROCEDURE enforce_max_players();

CREATE OR REPLACE FUNCTION enrollment_category_same_league() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.category_id IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM category c
        JOIN edition e ON e.id = c.edition_id
        WHERE c.id = NEW.category_id
          AND e.league_id = NEW.league_id
    ) THEN
        RAISE EXCEPTION 'enrollment category does not belong to the league';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER enrollment_category_same_league_trg
    BEFORE INSERT OR UPDATE ON enrollment_import
    FOR EACH ROW EXECUTE PROCEDURE enrollment_category_same_league();

CREATE OR REPLACE FUNCTION group_team_same_category() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM competition_group g
        JOIN team_entry te ON te.id = NEW.team_entry_id
        WHERE g.id = NEW.group_id
          AND g.category_id = te.category_id
    ) THEN
        RAISE EXCEPTION 'group and team entry belong to different categories';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER group_team_same_category_trg
    BEFORE INSERT OR UPDATE ON group_team
    FOR EACH ROW EXECUTE PROCEDURE group_team_same_category();

CREATE OR REPLACE FUNCTION matchday_phase_same_category() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM phase p
        WHERE p.id = NEW.phase_id
          AND p.category_id = NEW.category_id
    ) THEN
        RAISE EXCEPTION 'matchday phase does not belong to the category';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER matchday_phase_same_category_trg
    BEFORE INSERT OR UPDATE ON matchday
    FOR EACH ROW EXECUTE PROCEDURE matchday_phase_same_category();

CREATE OR REPLACE FUNCTION bracket_slot_same_category() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    phase_category uuid;
BEGIN
    SELECT category_id INTO phase_category FROM phase WHERE id = NEW.phase_id;
    IF NOT EXISTS (
        SELECT 1 FROM competition_group WHERE id = NEW.home_group_id AND category_id = phase_category
    ) OR NOT EXISTS (
        SELECT 1 FROM competition_group WHERE id = NEW.away_group_id AND category_id = phase_category
    ) THEN
        RAISE EXCEPTION 'bracket groups do not belong to the phase category';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER bracket_slot_same_category_trg
    BEFORE INSERT OR UPDATE ON bracket_slot
    FOR EACH ROW EXECUTE PROCEDURE bracket_slot_same_category();

CREATE OR REPLACE FUNCTION field_referee_same_category() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    matchday_category uuid;
BEGIN
    SELECT category_id INTO matchday_category FROM matchday WHERE id = NEW.matchday_id;
    IF NOT EXISTS (
        SELECT 1 FROM field WHERE id = NEW.field_id AND category_id = matchday_category
    ) OR NOT EXISTS (
        SELECT 1 FROM referee WHERE id = NEW.referee_id AND category_id = matchday_category
    ) THEN
        RAISE EXCEPTION 'field or referee does not belong to the matchday category';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER field_referee_same_category_trg
    BEFORE INSERT OR UPDATE ON field_matchday_referee
    FOR EACH ROW EXECUTE PROCEDURE field_referee_same_category();

CREATE OR REPLACE FUNCTION match_same_category() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    matchday_category uuid;
    home_category uuid;
    away_category uuid;
BEGIN
    SELECT category_id INTO matchday_category FROM matchday WHERE id = NEW.matchday_id;

    SELECT category_id INTO home_category FROM team_entry WHERE id = NEW.home_team_entry_id;
    SELECT category_id INTO away_category FROM team_entry WHERE id = NEW.away_team_entry_id;
    IF home_category IS DISTINCT FROM matchday_category OR away_category IS DISTINCT FROM matchday_category THEN
        RAISE EXCEPTION 'match teams do not belong to the matchday category';
    END IF;

    IF NEW.group_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM competition_group WHERE id = NEW.group_id AND category_id = matchday_category
    ) THEN
        RAISE EXCEPTION 'match group does not belong to the matchday category';
    END IF;

    IF NEW.field_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM field WHERE id = NEW.field_id AND category_id = matchday_category
    ) THEN
        RAISE EXCEPTION 'match field does not belong to the matchday category';
    END IF;

    IF NEW.referee_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM referee WHERE id = NEW.referee_id AND category_id = matchday_category
    ) THEN
        RAISE EXCEPTION 'match referee does not belong to the matchday category';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER match_same_category_trg
    BEFORE INSERT OR UPDATE ON match
    FOR EACH ROW EXECUTE PROCEDURE match_same_category();

CREATE OR REPLACE FUNCTION player_in_match() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    player_entry uuid;
BEGIN
    SELECT team_entry_id INTO player_entry FROM player WHERE id = NEW.player_id;
    IF NOT EXISTS (
        SELECT 1 FROM match m
        WHERE m.id = NEW.match_id
          AND player_entry IN (m.home_team_entry_id, m.away_team_entry_id)
    ) THEN
        RAISE EXCEPTION 'player is not on a team in the match';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER goal_player_in_match_trg
    BEFORE INSERT OR UPDATE ON goal
    FOR EACH ROW EXECUTE PROCEDURE player_in_match();

CREATE TRIGGER card_player_in_match_trg
    BEFORE INSERT OR UPDATE ON player_card
    FOR EACH ROW EXECUTE PROCEDURE player_in_match();

CREATE OR REPLACE FUNCTION suspension_card_matches_player() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM player_card
        WHERE id = NEW.trigger_card_id
          AND player_id = NEW.player_id
    ) THEN
        RAISE EXCEPTION 'suspension card does not belong to the player';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER suspension_card_matches_player_trg
    BEFORE INSERT OR UPDATE ON suspension
    FOR EACH ROW EXECUTE PROCEDURE suspension_card_matches_player();
