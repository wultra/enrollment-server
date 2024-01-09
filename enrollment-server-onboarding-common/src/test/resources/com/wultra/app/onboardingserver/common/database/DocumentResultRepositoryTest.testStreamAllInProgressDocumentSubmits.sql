-- Documents that have not been submitted yet (multiple providers).
INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('v1', 'a1', 'u1', 'p1', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now()),
    ('v2', 'a2', 'u2', 'p2', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now());

INSERT INTO es_document_verification(id, provider_name, activation_id, identity_verification_id, type, status, filename, used_for_verification, timestamp_created, timestamp_last_updated) VALUES
    ('d1', 'foreign', 'a1', 'v1', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f1', true, now(), now()),
    ('d2', 'mock',    'a2', 'v2', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f2', true, now(), now());

INSERT INTO es_document_result(id, document_verification_id, phase, timestamp_created) VALUES
    (1, 'd1', 'UPLOAD', now()),
    (2, 'd2', 'UPLOAD', now());
