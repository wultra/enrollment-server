INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created) VALUES
    ('p3', '{}', 'ACTIVATION_IN_PROGRESS', 0, '{}', now());

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('v3', 'a3', 'u3', 'p3', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now());

-- no documents submitted yet
