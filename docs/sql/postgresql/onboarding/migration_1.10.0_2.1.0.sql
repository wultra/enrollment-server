-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::1::Lubos Racansky
-- Create a new sequence es_onboarding_process_configuration_seq
CREATE SEQUENCE  IF NOT EXISTS es_onboarding_process_configuration_seq START WITH 1 INCREMENT BY 50 CACHE 20;

-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::2::Lubos Racansky
-- Create a new table es_onboarding_process_configuration
CREATE TABLE es_onboarding_process_configuration (id BIGINT DEFAULT nextval('es_onboarding_process_configuration_seq') NOT NULL, process_type VARCHAR(255) NOT NULL, config TEXT NOT NULL, CONSTRAINT es_onboarding_process_configuration_pkey PRIMARY KEY (id), CONSTRAINT es_onboarding_process_configuration_process_type_uk UNIQUE (process_type));

-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::3::Lubos Racansky
-- Create a new column process_config_id to es_onboarding_process
ALTER TABLE es_onboarding_process ADD process_config_id BIGINT;

ALTER TABLE es_onboarding_process ADD CONSTRAINT es_onboarding_process_process_config_id_fk FOREIGN KEY (process_config_id) REFERENCES es_onboarding_process_configuration (id);

-- Changeset enrollment-server-onboarding/2.0.x/20251219-process-target-activation-id.xml::1::Lubos Racansky
-- Create a new column target_activation_id to es_onboarding_process
ALTER TABLE es_onboarding_process ADD target_activation_id VARCHAR(36);

-- Changeset enrollment-server-onboarding/2.0.x/20260116-add-processed-document-data-table.xml::1::Michal Rozehnal
-- Create a new table es_processed_document_data
CREATE TABLE es_processed_document_data (id VARCHAR(36) NOT NULL, data BYTEA NOT NULL, data_type VARCHAR(32) NOT NULL, timestamp_created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT es_processed_document_data_pkey PRIMARY KEY (id));

-- Changeset enrollment-server-onboarding/2.0.x/20260116-add-processed-document-data-table.xml::2::Michal Rozehnal
-- Remove column activation_id from es_document_data
ALTER TABLE es_document_data DROP COLUMN activation_id;

-- Changeset enrollment-server-onboarding/2.0.x/20260116-add-processed-document-data-table.xml::3::Michal Rozehnal
-- Remove foreign key es_document_data_identity_verification_id_fk
ALTER TABLE es_document_data DROP CONSTRAINT es_document_data_identity_verification_id_fk;

-- Changeset enrollment-server-onboarding/2.0.x/20260116-add-processed-document-data-table.xml::4::Michal Rozehnal
-- Remove column identity_verification_id from es_document_data
ALTER TABLE es_document_data DROP COLUMN identity_verification_id;

-- Changeset enrollment-server-onboarding/2.0.x/20260116-add-processed-document-data-table.xml::5::Michal Rozehnal
-- Remove column filename from es_document_data
ALTER TABLE es_document_data DROP COLUMN filename;

-- Changeset enrollment-server-onboarding/2.0.x/20260116-add-processed-document-data-table.xml::6::Michal Rozehnal
-- Add index for table es_document_verification column upload_id
CREATE INDEX es_document_verification_upload_id_idx ON es_document_verification(upload_id);

-- Changeset enrollment-server-onboarding/2.0.x/20260126-selfie.xml::1::Lubos Racansky
-- Create a new sequence es_selfie_seq
CREATE SEQUENCE  IF NOT EXISTS es_selfie_seq START WITH 1 INCREMENT BY 50 CACHE 20;

-- Changeset enrollment-server-onboarding/2.0.x/20260126-selfie.xml::2::Lubos Racansky
-- Create a new table es_selfie
CREATE TABLE es_selfie (id BIGINT DEFAULT nextval('es_selfie_seq') NOT NULL, image BYTEA, identity_verification_id VARCHAR(36) NOT NULL, timestamp_created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT es_selfie_pkey PRIMARY KEY (id), CONSTRAINT es_selfie_identity_verification_id_fk FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification(id));

-- Changeset enrollment-server-onboarding/2.0.x/20260126-selfie.xml::3::Lubos Racansky
-- Create a new index es_selfie_identity_verification_id_idx
CREATE INDEX es_selfie_identity_verification_id_idx ON es_selfie(identity_verification_id);

-- Changeset enrollment-server-onboarding/2.0.x/20260126-selfie.xml::4::Lubos Racansky
-- Create a new index es_selfie_timestamp_created_idx
CREATE INDEX es_selfie_timestamp_created_idx ON es_selfie(timestamp_created);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::1::Lubos Racansky
-- Create a new table audit_log
CREATE TABLE audit_log (audit_log_id VARCHAR(36) NOT NULL, application_name VARCHAR(256) NOT NULL, audit_level VARCHAR(32) NOT NULL, audit_type VARCHAR(256), timestamp_created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(), message TEXT NOT NULL, exception_message TEXT, stack_trace TEXT, param TEXT, calling_class VARCHAR(256) NOT NULL, thread_name VARCHAR(256) NOT NULL, version VARCHAR(256), build_time TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT audit_log_pkey PRIMARY KEY (audit_log_id));

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::2::Lubos Racansky
-- Create a new table audit_log
CREATE TABLE audit_param (audit_log_id VARCHAR(36), timestamp_created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(), param_key VARCHAR(256), param_value VARCHAR(4000));

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::3::Lubos Racansky
-- Create a new index on audit_log(timestamp_created)
CREATE INDEX audit_log_timestamp ON audit_log(timestamp_created);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::4::Lubos Racansky
-- Create a new index on audit_log(application_name)
CREATE INDEX audit_log_application ON audit_log(application_name);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::5::Lubos Racansky
-- Create a new index on audit_log(audit_level)
CREATE INDEX audit_log_level ON audit_log(audit_level);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::6::Lubos Racansky
-- Create a new index on audit_log(audit_type)
CREATE INDEX audit_log_type ON audit_log(audit_type);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::7::Lubos Racansky
-- Create a new index on audit_param(audit_log_id)
CREATE INDEX audit_param_log ON audit_param(audit_log_id);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::8::Lubos Racansky
-- Create a new index on audit_param(timestamp_created)
CREATE INDEX audit_param_timestamp ON audit_param(timestamp_created);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::9::Lubos Racansky
-- Create a new index on audit_log(param_key)
CREATE INDEX audit_param_key ON audit_param(param_key);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::10::Lubos Racansky
-- Create a new index on audit_log(param_value)
CREATE INDEX audit_param_value ON audit_param(param_value);

-- Changeset enrollment-server-onboarding/2.0.x/20260309-processed-document-data-verification-id.xml::1::Michal Rozehnal
-- Creates a new column document_verification_id in es_processed_document_data
ALTER TABLE es_processed_document_data ADD document_verification_id VARCHAR(36);

ALTER TABLE es_processed_document_data ADD CONSTRAINT es_processed_document_data_document_verification_id_fk FOREIGN KEY (document_verification_id) REFERENCES es_document_verification (id);

-- Changeset enrollment-server-onboarding/2.0.x/20260309-processed-document-data-verification-id.xml::2::Michal Rozehnal
-- Creates a new index es_processed_document_data_document_verification_id_idx
CREATE INDEX es_processed_document_data_document_verification_id_idx ON es_processed_document_data(document_verification_id);

-- Changeset enrollment-server-onboarding/2.0.x/20260313-consent-accepted.xml::1::Michal Rozehnal
-- Creates a new column consent_accepted in es_onboarding_process
ALTER TABLE es_onboarding_process ADD consent_accepted BOOLEAN;

-- Changeset enrollment-server-onboarding/2.0.x/20260402-document-country.xml::1::Lubos Racansky
-- Creates a new column country in es_document_verification
ALTER TABLE es_document_verification ADD country VARCHAR(3);

-- Changeset enrollment-server-onboarding/2.0.x/20251215-add-tag-2.0.0.xml::1::Lubos Racansky
-- Changeset enrollment-server-onboarding/2.1.x/20260330-audit-subject-id.xml::1::Pavel Sindelar
-- Add subject_id column to audit_log table
ALTER TABLE audit_log ADD subject_id VARCHAR(256);

-- Changeset enrollment-server-onboarding/2.1.x/20260330-audit-subject-id.xml::2::Pavel Sindelar
-- Create a new index on audit_log(subject_id)
CREATE INDEX audit_log_subject_id_idx ON audit_log(subject_id);

-- Changeset enrollment-server-onboarding/2.1.x/20260423-document-result-anonymized-column.xml::1::Michal Rozehnal
-- Add column anonymized into table es_document_result
ALTER TABLE es_document_result ADD anonymized BOOLEAN DEFAULT TRUE NOT NULL;

-- Changeset enrollment-server-onboarding/2.1.x/20260423-document-result-anonymized-column.xml::2::Michal Rozehanal
-- Change default value of es_document_result.anonymized to false
ALTER TABLE es_document_result ALTER COLUMN  anonymized SET DEFAULT FALSE;

-- Changeset enrollment-server-onboarding/2.1.x/20260423-document-result-anonymized-column.xml::3::Michal Rozehnal
-- Add index on anonymized column in es_document_result table
CREATE INDEX es_document_result_anonymized_idx ON es_document_result(anonymized);
