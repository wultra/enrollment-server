INSERT INTO es_onboarding_process(id, identification_data, status, error_score, custom_data, timestamp_created) VALUES
    ('b4662611-df91-4053-bb3d-3970979baf5d', '{}', 'VERIFICATION_IN_PROGRESS', 0, '{}', now());

INSERT INTO es_onboarding_otp(id, process_id, otp_code, failed_attempts, total_attempts, status, type, timestamp_created, timestamp_expiration) VALUES
    ('f50b8c04-649d-43a7-8079-4dbf9b0bbc72', 'b4662611-df91-4053-bb3d-3970979baf5d', '123', 0, 0, 'ACTIVE', 'USER_VERIFICATION', now(), now()),
    ('6560a85d-7d97-44c0-bd29-04c57051aa57', 'b4662611-df91-4053-bb3d-3970979baf5d', '123', 0, 0, 'ACTIVE', 'USER_VERIFICATION', now() - interval '301' second, now()), -- to be failed
    ('e4974ef6-135a-4ae1-be91-a2c0f674c8fd', 'b4662611-df91-4053-bb3d-3970979baf5d', '123', 0, 0, 'VERIFIED', 'USER_VERIFICATION', now() - interval '301' second, now());
