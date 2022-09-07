INSERT INTO es_onboarding_process(id, identification_data, status, error_score, timestamp_created) VALUES
    ('11111111-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', 0, now() - interval '3' hour),
    ('22222222-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', 0, now()),
    ('33333333-df91-4053-bb3d-3970979baf5d', '{}', 'FINISHED', 0, now() - interval '5' minute);
