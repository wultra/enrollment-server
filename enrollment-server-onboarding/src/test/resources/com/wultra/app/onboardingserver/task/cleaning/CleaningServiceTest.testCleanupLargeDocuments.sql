RUNSCRIPT FROM 'classpath:/com/wultra/app/onboardingserver/task/cleaning/CleaningServiceTest.testTerminateExpiredIdentityVerifications.sql';

INSERT INTO es_document_data(id, activation_id, identity_verification_id, filename, data, timestamp_created) VALUES
    ('93a41939-a808-4fe4-a673-f527a294f33e', 'a1', 'a6055e8b-4ac0-45dd-b68e-29f4cd991a5c', 'f1', 'data1', now()),
    ('54bcf744-3e78-4a17-b84e-eea065d733a6', 'a2', '8d036a18-f51f-4a30-92cd-04876172ebca', 'f2', 'data2', now() - 1); -- to be deleted
