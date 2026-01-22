INSERT INTO es_onboarding_process_configuration (id, process_type, config)
VALUES (3, 'onboardingSimple', '{"enabled":true,"activationType":"CODE","otpForIdentification":false,"otpForIdentityVerification":false,"documents":{"totalRequiredDocumentsCount":1,"groups":[{"requiredDocumentsCount":1,"items":[{"type":"ID_CARD","sideCount":2}]}]}}');

INSERT INTO es_onboarding_process (id, identification_data, user_id, activation_id, status, activation_removed, error_detail, error_origin, error_score, custom_data, timestamp_created, timestamp_last_updated, timestamp_finished, timestamp_failed, fds_data, process_config_id, target_activation_id)
VALUES ('07e59ba8-dc12-4c2c-a294-d9acb0b6a2b7', '{"birthDate" : "1970-03-21","clientNumber" : "977952496748934771"}', 'mockuser_977952496748934771', '26c98f91-e373-4bef-8704-7224880a9912', 'VERIFICATION_IN_PROGRESS', false, null, null, 0, '{"locale":"cs","ipAddress":"127.0.0.1","userAgent":"ReactorNetty/1.2.12"}', '2026-01-19 14:10:30.771000', '2026-01-19 14:10:30.948000', null, null, null, 3, null);

INSERT INTO es_identity_verification (id, activation_id, user_id, process_id, status, phase, reject_reason, reject_origin, error_detail, error_origin, session_info, timestamp_created, timestamp_last_updated)
VALUES ('e0a627b9-9829-4bec-8c8d-db3be4ff03c1', '26c98f91-e373-4bef-8704-7224880a9912', 'mockuser_977952496748934771', '07e59ba8-dc12-4c2c-a294-d9acb0b6a2b7', 'VERIFICATION_PENDING', 'DOCUMENT_UPLOAD', null, null, null, null, null, '2026-01-19 14:10:30.940000', '2026-01-19 14:10:33.011000');

