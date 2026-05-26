-- p_fail: active process → v_fail will trigger RuntimeException via spy on onboardingProcessRepository
INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created) VALUES
    ('p_fail', '{}', 'ACTIVATION_IN_PROGRESS', 0, '{}', now());

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('v_fail', 'a_fail', 'u_fail', 'p_fail', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now());

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, provider_name, status, filename, used_for_verification, timestamp_created, timestamp_last_updated) VALUES
    ('doc_fail', 'a_fail', 'v_fail', 'ID_CARD', 'mock', 'VERIFICATION_PENDING', 'f_fail', true, now(), now());

