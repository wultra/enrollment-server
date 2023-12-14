INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created) VALUES
    ('v1', 'a1', 'u1', 'p1', 'VERIFICATION_PENDING', 'DOCUMENT_VERIFICATION', now());

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, side, status, used_for_verification, filename, timestamp_created) VALUES
    ('original1', 'a1', 'v1', 'ID_CARD', 'FRONT', 'VERIFICATION_PENDING', true, 'original_id_front.png', now()),
    ('original2', 'a1', 'v1', 'ID_CARD',  'BACK', 'VERIFICATION_PENDING', true, 'original_id_back.png', now());