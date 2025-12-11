INSERT INTO es_onboarding_process_configuration (id, process_type, config) VALUES
    (1, 'reactivation', '{"enabled":true,"activationType":"CODE","otpForIdentification":true,"otpForIdentityVerification":true,"documents":{"requiredTotalDocumentsCount":2,"requiredPrimaryDocumentsCount":"1","items":[{"type":"ID_CARD","sideCount":2,"obligation":["PRIMARY"]},{"type":"DRIVING_LICENCE","sideCount":1}]}}'),
    (2, 'onboarding', '{}');
