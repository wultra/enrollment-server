RUNSCRIPT FROM 'classpath:/com/wultra/app/onboardingserver/task/cleaning/CleaningTaskTest.testTerminateExpiredIdentityVerifications.sql';

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, status, filename, used_for_verification, timestamp_created, timestamp_last_updated) VALUES
    ('16055e8b-4ac0-45dd-b68e-29f4cd991a5c', 'a1', 'a6055e8b-4ac0-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f1', false, now(), now()),
    ('2d036a18-f51f-4a30-92cd-04876172ebca', 'a2', '8d036a18-f51f-4a30-92cd-04876172ebca', 'ID_CARD', 'UPLOAD_IN_PROGRESS', 'f2', false, now() - 1, now() - 1), -- to be terminated
    ('3918e1c4-5ca7-47da-8765-afc92082f717', 'a3', 'c918e1c4-5ca7-47da-8765-afc92082f717', 'ID_CARD', 'ACCEPTED', 'f3', true, now() - 1, now() - 1);
