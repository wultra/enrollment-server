INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated) VALUES
    ('11111111-4ac0-45dd-b68e-29f4cd991a5c', 'a1', 'u1', 'p1', 'IN_PROGRESS', 'PRESENCE_CHECK', now(), now()),
    ('22222222-4ac0-45dd-b68e-29f4cd991a5c', 'a2', 'u2', 'p2', 'IN_PROGRESS', 'PRESENCE_CHECK', now(), now());

INSERT INTO es_selfie(id, identity_verification_id, timestamp_created) VALUES
    (1, '11111111-4ac0-45dd-b68e-29f4cd991a5c', DATEADD('HOUR', -3, NOW())),
    (2, '22222222-4ac0-45dd-b68e-29f4cd991a5c', DATEADD('HOUR', -5, NOW()));  -- to be deleted

