INSERT INTO es_onboarding_process_configuration (id, process_type, config) VALUES
    (1, 'reactivation', '{"enabled":true,"otpForIdentification":true,"otpForIdentityVerification":true,"documents":{"requiredDocumentsCount":2,"items":[{"type":"ID_CARD","sideCount":2,"mandatory":true},{"type":"DRIVING_LICENCE","sideCount":1,"mandatory":false}]}}'),
    (2, 'onboarding', '{}');

INSERT INTO es_onboarding_process(id, identification_data, status, activation_id, activation_removed, error_score, custom_data, timestamp_created) VALUES
    ('00000000-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', null, false, 0, '{}', now()),
    ('11111111-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', 'a1', false, 0, '{}', now()),
    ('22222222-df91-4053-bb3d-3970979baf5d', '{}', 'VERIFICATION_IN_PROGRESS', 'a2', false, 0, '{}', now()),
    ('33333333-df91-4053-bb3d-3970979baf5d', '{}', 'FAILED', 'a3', true, 0, '{}', now());
