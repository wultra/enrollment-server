INSERT INTO es_onboarding_process(id, identification_data, status, activation_id, activation_removed, error_score, custom_data, timestamp_created) VALUES
    ('11111111-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', 'a1', false, 0, '{}', now()),
    ('22222222-df91-4053-bb3d-3970979baf5d', '{}', 'FAILED', 'a2', false, 0, '{}', now()),
    ('33333333-df91-4053-bb3d-3970979baf5d', '{}', 'FAILED', 'a3', true, 0, '{}', now());
