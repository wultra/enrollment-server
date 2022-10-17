INSERT INTO es_onboarding_process(id, identification_data, status, error_score, timestamp_created) VALUES
    ('11111111-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', 0, now() - interval '301' second),
    ('22222222-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', 0, now() - interval '301' second),
    ('33333333-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', 0, now() - interval '301' second),
    ('44444444-df91-4053-bb3d-3970979baf5d', '{}', 'ACTIVATION_IN_PROGRESS', 0, now()); -- to be kept

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('11111111-4ac0-45dd-b68e-29f4cd991a5c', 'a1', 'u1', '11111111-df91-4053-bb3d-3970979baf5d', 'IN_PROGRESS', 'PRESENCE_CHECK', now(), now()),
    ('22222222-4ac0-45dd-b68e-29f4cd991a5c', 'a2', 'u2', '22222222-df91-4053-bb3d-3970979baf5d', 'IN_PROGRESS', 'PRESENCE_CHECK', now(), now()),
    ('33333333-4ac0-45dd-b68e-29f4cd991a5c', 'a3', 'u3', '33333333-df91-4053-bb3d-3970979baf5d', 'ACCEPTED', 'COMPLETED', now(), now()), -- do not change even process failed
    ('44444444-4ac0-45dd-b68e-29f4cd991a5c', 'a3', 'u3', '44444444-df91-4053-bb3d-3970979baf5d', 'IN_PROGRESS', 'PRESENCE_CHECK', now(), now());

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, status, filename, used_for_verification, timestamp_created, timestamp_last_updated) VALUES
    ('11111111-f51f-4a30-92cd-04876172ebca', 'a1', '11111111-4ac0-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f1', false, now(), now()), -- do not change even identity verification failed
    ('22222222-f51f-4a30-92cd-04876172ebca', 'a2', '22222222-4ac0-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'ACCEPTED', 'f2', false, now(), now()),
    ('33333333-f51f-4a30-92cd-04876172ebca', 'a3', '33333333-4ac0-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f3', true, now(), now()),
    ('44444444-f51f-4a30-92cd-04876172ebca', 'a3', '44444444-4ac0-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'ACCEPTED', 'f3', true, now(), now());
