-- failed process eligible for cleanup
INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created, timestamp_finished, timestamp_failed)
VALUES
	('11111111-aaaa-4053-bb3d-3970979baf5d', '{}', 'FAILED', 0, '{}', now() - interval '7200' second, null, now() - interval '7200' second);

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated, timestamp_finished, timestamp_failed)
VALUES
	('11111111-bbbb-45dd-b68e-29f4cd991a5c', 'cleanup-a1', 'cleanup-u1', '11111111-aaaa-4053-bb3d-3970979baf5d', 'FAILED', 'COMPLETED', now() - interval '7200' second, now() - interval '7200' second, null, now() - interval '7200' second);

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, status, filename, upload_id, used_for_verification, timestamp_created, timestamp_last_updated)
VALUES
	('11111111-cccc-4a30-92cd-04876172ebca', 'cleanup-a1', '11111111-bbbb-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'FAILED', 'failed.png', 'upload-cleanup-failed', true, now() - interval '7200' second, now() - interval '7200' second);

INSERT INTO es_document_data(id, data, timestamp_created, document_verification_id)
VALUES
	('upload-cleanup-failed', 'failed-document-data', now() - interval '7200' second, '11111111-cccc-4a30-92cd-04876172ebca');

INSERT INTO es_processed_document_data(id, data, data_type, timestamp_created, document_verification_id)
VALUES
	('processed-cleanup-failed', 'failed-processed-document', 'FACE_IMAGE', now() - interval '7200' second, '11111111-cccc-4a30-92cd-04876172ebca');

INSERT INTO es_selfie(id, image, identity_verification_id, timestamp_created)
VALUES
	(1, X'01', '11111111-bbbb-45dd-b68e-29f4cd991a5c', now() - interval '7200' second);

-- successful process eligible for cleanup with 1 disposed document
INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created, timestamp_finished, timestamp_failed)
VALUES
	('22222222-aaaa-4053-bb3d-3970979baf5d', '{}', 'FINISHED', 0, '{}', now() - interval '7200' second, now() - interval '7200' second, null);

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated, timestamp_finished, timestamp_failed)
VALUES
	('22222222-bbbb-45dd-b68e-29f4cd991a5c', 'cleanup-a2', 'cleanup-u2', '22222222-aaaa-4053-bb3d-3970979baf5d', 'ACCEPTED', 'COMPLETED', now() - interval '7200' second, now() - interval '7200' second, now() - interval '7200' second, null);

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, status, filename, upload_id, used_for_verification, timestamp_created, timestamp_last_updated)
VALUES
	('22222222-cccc-4a30-92cd-04876172ebca', 'cleanup-a2', '22222222-bbbb-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'ACCEPTED', 'success.png', 'upload-cleanup-success', true, now() - interval '7200' second, now() - interval '7200' second),
	('22222222-dddd-4a30-92cd-04876172ebca', 'cleanup-a2', '22222222-bbbb-45dd-b68e-29f4cd991a5c', 'PASSPORT', 'DISPOSED', 'disposed.png', 'upload-cleanup-success-disposed', false, now() - interval '7200' second, now() - interval '7200' second);

INSERT INTO es_document_data(id, data, timestamp_created, document_verification_id)
VALUES
	('upload-cleanup-success', 'successful-document-data', now() - interval '7200' second, '22222222-cccc-4a30-92cd-04876172ebca'),
    ('upload-cleanup-success-disposed', 'successful-document-data-disposed', now() - interval '7200' second, '22222222-dddd-4a30-92cd-04876172ebca');

INSERT INTO es_processed_document_data(id, data, data_type, timestamp_created, document_verification_id)
VALUES
	('processed-cleanup-success', 'successful-processed-document', 'FACE_IMAGE', now() - interval '7200' second, '22222222-cccc-4a30-92cd-04876172ebca'),
    ('processed-cleanup-success-disposed', 'successful-processed-document-disposed', 'FACE_IMAGE', now() - interval '7200' second, '22222222-dddd-4a30-92cd-04876172ebca');

INSERT INTO es_selfie(id, image, identity_verification_id, timestamp_created)
VALUES
	(2, X'02', '22222222-bbbb-45dd-b68e-29f4cd991a5c', now() - interval '7200' second);

-- in progress process kept intact
INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created, timestamp_finished, timestamp_failed)
VALUES
	('33333333-aaaa-4053-bb3d-3970979baf5d', '{}', 'VERIFICATION_IN_PROGRESS', 0, '{}', now() - interval '7200' second, null, null);

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated, timestamp_finished, timestamp_failed)
VALUES
	('33333333-bbbb-45dd-b68e-29f4cd991a5c', 'cleanup-a3', 'cleanup-u3', '33333333-aaaa-4053-bb3d-3970979baf5d', 'IN_PROGRESS', 'DOCUMENT_VERIFICATION_FINAL', now() - interval '7200' second, now() - interval '7200' second, null, null);

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, status, filename, upload_id, used_for_verification, timestamp_created, timestamp_last_updated)
VALUES
	('33333333-cccc-4a30-92cd-04876172ebca', 'cleanup-a3', '33333333-bbbb-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'VERIFICATION_PENDING', 'progress.png', 'upload-cleanup-progress', true, now() - interval '7200' second, now() - interval '7200' second);

INSERT INTO es_document_data(id, data, timestamp_created, document_verification_id)
VALUES
	('upload-cleanup-progress', 'in-progress-document-data', now() - interval '7200' second, '33333333-cccc-4a30-92cd-04876172ebca');

INSERT INTO es_processed_document_data(id, data, data_type, timestamp_created, document_verification_id)
VALUES
	('processed-cleanup-progress', 'in-progress-processed-document', 'FACE_IMAGE', now() - interval '7200' second, '33333333-cccc-4a30-92cd-04876172ebca');

INSERT INTO es_selfie(id, image, identity_verification_id, timestamp_created)
VALUES
	(3, X'03', '33333333-bbbb-45dd-b68e-29f4cd991a5c', now() - interval '7200' second);

-- recent failed process not eligible for cleanup
INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created, timestamp_finished, timestamp_failed)
VALUES
	('44444444-aaaa-4053-bb3d-3970979baf5d', '{}', 'FAILED', 0, '{}', now() - interval '1800' second, null, now() - interval '1800' second);

INSERT INTO es_identity_verification(id, activation_id, user_id, process_id, status, phase, timestamp_created, timestamp_last_updated, timestamp_finished, timestamp_failed)
VALUES
	('44444444-bbbb-45dd-b68e-29f4cd991a5c', 'cleanup-a4', 'cleanup-u4', '44444444-aaaa-4053-bb3d-3970979baf5d', 'FAILED', 'COMPLETED', now() - interval '1800' second, now() - interval '1800' second, null, now() - interval '1800' second);

INSERT INTO es_document_verification(id, activation_id, identity_verification_id, type, status, filename, upload_id, used_for_verification, timestamp_created, timestamp_last_updated)
VALUES
	('44444444-cccc-4a30-92cd-04876172ebca', 'cleanup-a4', '44444444-bbbb-45dd-b68e-29f4cd991a5c', 'ID_CARD', 'FAILED', 'recent-failed.png', 'upload-cleanup-recent-failed', true, now() - interval '1800' second, now() - interval '1800' second);

INSERT INTO es_document_data(id, data, timestamp_created, document_verification_id)
VALUES
	('upload-cleanup-recent-failed', 'recent-failed-document-data', now() - interval '1800' second, '44444444-cccc-4a30-92cd-04876172ebca');

INSERT INTO es_processed_document_data(id, data, data_type, timestamp_created, document_verification_id)
VALUES
	('processed-cleanup-recent-failed', 'recent-failed-processed-document', 'FACE_IMAGE', now() - interval '1800' second, '44444444-cccc-4a30-92cd-04876172ebca');

INSERT INTO es_selfie(id, image, identity_verification_id, timestamp_created)
VALUES
	(4, X'04', '44444444-bbbb-45dd-b68e-29f4cd991a5c', now() - interval '1800' second);

