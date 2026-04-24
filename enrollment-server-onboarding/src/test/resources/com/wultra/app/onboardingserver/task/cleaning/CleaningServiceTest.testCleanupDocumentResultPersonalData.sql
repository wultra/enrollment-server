INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('11111111-4ac0-45dd-b68e-29f4cd991a5c', 'a1', 'u1', 'p1', 'IN_PROGRESS', 'PRESENCE_CHECK', now(), now());

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, side, status, filename, upload_id, timestamp_created, used_for_verification) VALUES
    ('11111111-f51f-4a30-92cd-04876172ebca', 'a1', '11111111-4ac0-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'FRONT', 'UPLOAD_IN_PROGRESS', 'id-1.jpg', 'upload-1', now(), true),
    ('22222222-f51f-4a30-92cd-04876172ebca', 'a1', '11111111-4ac0-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'BACK', 'UPLOAD_IN_PROGRESS', 'id-2.jpg', 'upload-2', now(), true);

INSERT INTO es_document_result(id, document_verification_id, phase, verification_result, extracted_data, timestamp_created, anonymized) VALUES
    (1001, '11111111-f51f-4a30-92cd-04876172ebca', 'UPLOAD', '{}', '{}', DATEADD('HOUR', -1, NOW()), false), -- should not be cleaned
    (1002, '22222222-f51f-4a30-92cd-04876172ebca', 'UPLOAD', '{}', '{}', DATEADD('HOUR', -5, NOW()), false); -- should be cleaned

