INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('v1', 'a1', 'u1', 'p1', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now()),
    ('v2', 'a2', 'u2', 'p2', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now(), now());

INSERT INTO es_document_verification(id, provider_name, activation_id, identity_verification_id, type, status, filename, used_for_verification, timestamp_created, timestamp_last_updated) VALUES
    ('d11', 'zenid',       'a1', 'v1', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f11', true, now(), now()),
    ('d12', 'zenid',       'a1', 'v1', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f12', true, now(), now()),
    ('d21', 'innovatrics', 'a2', 'v2', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f21', true, now(), now()),
    ('d22', 'innovatrics', 'a2', 'v2', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f22', true, now(), now()),
    ('d23', 'innovatrics', 'a2', 'v2', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f23', true, now(), now());

INSERT INTO es_document_result(id, document_verification_id, phase, timestamp_created) VALUES
    (11, 'd11', 'UPLOAD', now()),
    (12, 'd12', 'UPLOAD', now()),
    (21, 'd21', 'UPLOAD', now()),
    (22, 'd22', 'UPLOAD', now()),
    (23, 'd23', 'UPLOAD', now());
