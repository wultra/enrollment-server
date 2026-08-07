INSERT INTO es_onboarding_process_configuration (id, process_type, config) VALUES
    (1, 'reactivation', '{"enabled":true,"activationType":"CODE","otpForIdentification":true,"otpForIdentityVerification":true,"documents":{"totalRequiredDocumentsCount":2,"groups":[{"requiredDocumentsCount":1,"items":[{"type":"ID_CARD","sideCount":2}]},{"requiredDocumentsCount":0,"items":[{"type":"DRIVING_LICENSE","sideCount":1},{"type":"PASSPORT","sideCount":1}]}]}}'),
    (2, 'onboarding', '{"enabled":true,"consentRequired":true}'),
    (3, 're-kyc', '{"enabled":true,"existingActivation":true,"existingActivationFlag":"RE_KYC_IN_PROGRESS","invalidateExistingActivationOnFailure":false}');
