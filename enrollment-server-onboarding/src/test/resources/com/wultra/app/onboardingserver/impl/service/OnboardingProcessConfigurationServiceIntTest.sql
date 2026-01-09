INSERT INTO es_onboarding_process_configuration (id, process_type, config) VALUES
 (3, 'test-process-config', '{}');

INSERT INTO es_onboarding_process (id, identification_data, activation_id, status, custom_data, timestamp_created, error_score, process_config_id) values
 ( '6f1c9e4a-8b7d-4c3f-9a21-2e7c5f4b1d93', '{}', 'a9d3f4c2-1b7e-4e6a-8c5d-0f2e9b6a7c31', 'VERIFICATION_IN_PROGRESS', '{}', now(), 0, null),
 ( 'c2a8f7d1-3e6b-4f5a-9d84-1b0e6c9a2f47', '{}', '4e2b8d9f-6c1a-4f73-b5e0-9a7c3d1f2e84', 'VERIFICATION_IN_PROGRESS', '{}', now(), 0, 3);
