INSERT INTO es_onboarding_process_configuration (id, process_type, config) VALUES
    (1, 'onboarding', '{}');

INSERT INTO es_onboarding_process(id, process_config_id, identification_data, status, error_score, custom_data, timestamp_created) VALUES
    ('11111111-df91-4053-bb3d-3970979baf5d', 1, '{}', 'ACTIVATION_IN_PROGRESS', 0, '{}', now() - interval '10801' second),
    ('22222222-df91-4053-bb3d-3970979baf5d', 1, '{}', 'ACTIVATION_IN_PROGRESS', 0, '{}', now()),
    ('33333333-df91-4053-bb3d-3970979baf5d', 1, '{}', 'FINISHED', 0, '{}', now() - interval '301' second);
