INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created) VALUES
    ('p2', '{}', 'ACTIVATION_IN_PROGRESS', 0, '{}', now());

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('v2', 'a2', 'u2', 'p2', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now());

-- document is being submitted to a provider
INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, provider_name, status, filename, used_for_verification, timestamp_created, timestamp_last_updated) VALUES
    ('doc2', 'a2', 'v2', 'ID_CARD', null, 'UPLOAD_IN_PROGRESS', 'f1', true, now(), now());
