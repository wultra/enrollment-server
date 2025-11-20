INSERT INTO es_onboarding_process_configuration (id, process_type, config) VALUES
    (1, 'reactivation', '{"enabled":true,"otpForIdentification":true,"otpForIdentityVerification":true,"documents":{"requiredDocumentsCount":2,"items":[{"type":"ID_CARD","sideCount":2,"mandatory":true},{"type":"DRIVING_LICENCE","sideCount":1,"mandatory":false}]}}'),
    (2, 'onboarding', '{}');