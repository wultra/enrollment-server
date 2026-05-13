INSERT INTO es_onboarding_process_configuration (id, process_type, config) VALUES
    (1, 'reactivation', '{"enabled":true,"activationType":"CODE","otpForIdentification":true,"otpForIdentityVerification":true,"useTemporaryActivation":true,"approvalEnabled":true,"verifyPresenceWithOtp":false,"consentRequired":true,"documents":{"totalRequiredDocumentsCount":2,"groups":[{"requiredDocumentsCount":1,"items":[{"type":"ID_CARD","sideCount":2},{"type":"PASSPORT","sideCount":1}]},{"requiredDocumentsCount":0,"items":[{"type":"DRIVING_LICENSE","sideCount":1}]}]}}'),
    (2, 'onboarding', '{"unknown":"value"}'),
    (3, 'invalid', '{"documents":{"totalRequiredDocumentsCount":1,"groups":[{"requiredDocumentsCount":1,"items":[{"type":"ID_CARD","sideCount":-1}]}]}}');
