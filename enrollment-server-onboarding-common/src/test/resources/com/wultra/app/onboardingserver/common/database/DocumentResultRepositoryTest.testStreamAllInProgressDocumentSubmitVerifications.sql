-- Documents that have been already submitted and data were extracted (multiple providers).
INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
('v3', 'a3', 'u3', 'p3', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now()),
('v4', 'a4', 'u4', 'p4', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now());

INSERT INTO es_document_verification(id, provider_name, activation_id, identity_verification_id, verification_id, type, status, filename, used_for_verification, timestamp_created, timestamp_last_updated) VALUES
    ('d3', 'foreign', 'a3', 'v3', 'verification1', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f3', true, now(), now()),
    ('d4', 'mock',    'a4', 'v4', 'verification2', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f4', true, now(), now());

INSERT INTO es_document_result(id, document_verification_id, phase, extracted_data, timestamp_created) VALUES
    (3, 'd3', 'UPLOAD', '{extracted_data}', now()),
    (4, 'd4', 'UPLOAD', '{extracted_data}', now());
