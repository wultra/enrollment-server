INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created) VALUES
    ('p1', '{}', 'ACTIVATION_IN_PROGRESS', 0, '{}', now());

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('v1', 'a1', 'u1', 'p1', 'IN_PROGRESS', 'ACTIVATION_FINISH', now(), now());

-- document already submitted to 'mock' provider
INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, provider_name, status, filename, used_for_verification, timestamp_created, timestamp_last_updated) VALUES
    ('doc1', 'a1', 'v1', 'ID_CARD', 'mock', 'VERIFICATION_PENDING', 'f2', true, now(), now());
