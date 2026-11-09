INSERT INTO league (id, name, valid_from, valid_until, active)
VALUES ('11111111-1111-1111-1111-111111111111', 'Liga demo', DATE '2026-01-01', DATE '2027-12-31', true);

INSERT INTO league_settings (league_id, suspension_timing, fair_play_yellow_points, fair_play_red_points)
VALUES ('11111111-1111-1111-1111-111111111111', 'NEXT_MATCHDAY', 1, 3);

INSERT INTO app_user (id, username, password_hash, email, display_name, master)
VALUES
    ('22222222-2222-2222-2222-222222222221', 'master', '$2a$10$MOxJwT.GTSLrQ78WoBSnQOedNWwtmG2nNMudVVtbp3PfLA9gtdPwq', 'master@away.local', 'Master', true),
    ('22222222-2222-2222-2222-222222222222', 'admin', '$2a$10$iO3s2Y22j9t8Q6.UJ6NAO.PHI1qsgtt7rZAwOE3tB.IWzOb7IH.o2', 'admin@away.local', 'Administrativo', false),
    ('22222222-2222-2222-2222-222222222223', 'asistente', '$2a$10$oxAxuGnlt9QK82H31FB78.2HtoPiE6HXIoAeHRGh1SPWFFkYOzBme', 'asistente@away.local', 'Asistente', false);

INSERT INTO league_membership (id, user_id, league_id, role)
VALUES
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'ADMINISTRATIVO'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', 'ASISTENTE');
