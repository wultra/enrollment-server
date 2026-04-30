/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

--
--  Create sequences. Maximum value for PostgreSQL is 9223372036854775807.
--- See: https://www.postgresql.org/docs/9.6/sql-createsequence.html
--
CREATE SEQUENCE es_document_result_seq MINVALUE 1 MAXVALUE 9223372036854775807 INCREMENT BY 10 START WITH 1 CACHE 20;

CREATE TABLE es_onboarding_process (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    identification_data VARCHAR(1024) NOT NULL,
    user_id VARCHAR(256),
    activation_id VARCHAR(36),
    status VARCHAR(32) NOT NULL,
    activation_removed BOOLEAN DEFAULT FALSE,
    error_detail VARCHAR(256),
    error_origin VARCHAR(256),
    error_score INTEGER NOT NULL DEFAULT 0,
    custom_data VARCHAR(1024) NOT NULL,
    fds_data TEXT,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated TIMESTAMP,
    timestamp_finished TIMESTAMP,
    timestamp_failed TIMESTAMP
);

CREATE INDEX onboarding_process_status ON es_onboarding_process (status);
CREATE INDEX onboarding_process_identif_data ON es_onboarding_process (identification_data);
CREATE INDEX onboarding_process_timestamp_1 ON es_onboarding_process (timestamp_created);
CREATE INDEX onboarding_process_timestamp_2 ON es_onboarding_process (timestamp_last_updated);

CREATE TABLE es_onboarding_otp (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    process_id VARCHAR(36) NOT NULL,
    identity_verification_id VARCHAR(36),
    otp_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    error_detail VARCHAR(256),
    error_origin VARCHAR(256),
    failed_attempts INTEGER,
    total_attempts INTEGER DEFAULT 0,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_expiration TIMESTAMP NOT NULL,
    timestamp_last_updated TIMESTAMP,
    timestamp_verified TIMESTAMP,
    timestamp_failed TIMESTAMP,
    FOREIGN KEY (process_id) REFERENCES es_onboarding_process (id),
    FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification (id)
);

-- PostgreSQL does not create indexes on foreign keys automatically
CREATE INDEX onboarding_process ON es_onboarding_otp (process_id);
CREATE INDEX onboarding_otp_status ON es_onboarding_otp (status);
CREATE INDEX onboarding_otp_timestamp_1 ON es_onboarding_otp (timestamp_created);
CREATE INDEX onboarding_otp_timestamp_2 ON es_onboarding_otp (timestamp_last_updated);

CREATE TABLE es_identity_verification (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(256) NOT NULL,
    process_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    reject_reason TEXT,
    reject_origin VARCHAR(256),
    error_detail VARCHAR(256),
    error_origin VARCHAR(256),
    session_info TEXT,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated TIMESTAMP,
    timestamp_finished TIMESTAMP,
    timestamp_failed TIMESTAMP,
    FOREIGN KEY (process_id) REFERENCES es_onboarding_process (id)
);

CREATE INDEX identity_verif_activation ON es_identity_verification (activation_id);
CREATE INDEX identity_verif_user ON es_identity_verification (user_id);
CREATE INDEX identity_verif_status ON es_identity_verification (status);
CREATE INDEX identity_verif_phase ON es_identity_verification (phase);
CREATE INDEX identity_verif_timestamp_1 ON es_identity_verification (timestamp_created);
CREATE INDEX identity_verif_timestamp_2 ON es_identity_verification (timestamp_last_updated);

CREATE TABLE es_document_verification (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activation_id VARCHAR(36) NOT NULL,
    identity_verification_id VARCHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    side VARCHAR(5),
    other_side_id VARCHAR(36),
    provider_name VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    filename VARCHAR(256) NOT NULL,
    upload_id VARCHAR(36),
    verification_id VARCHAR(36),
    photo_id VARCHAR(256),
    verification_score INTEGER,
    reject_reason TEXT,
    reject_origin VARCHAR(256),
    error_detail VARCHAR(256),
    error_origin VARCHAR(256),
    original_document_id VARCHAR(36),
    used_for_verification BOOLEAN DEFAULT FALSE,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_uploaded TIMESTAMP,
    timestamp_verified TIMESTAMP,
    timestamp_disposed TIMESTAMP,
    timestamp_last_updated TIMESTAMP,
    FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification (id)
);

-- PostgreSQL does not create indexes on foreign keys automatically
CREATE INDEX document_ident_verif ON es_document_verification (identity_verification_id);
CREATE INDEX document_verif_activation ON es_document_verification (activation_id);
CREATE INDEX document_verif_status ON es_document_verification (status);
CREATE INDEX document_verif_timestamp_1 ON es_document_verification (timestamp_created);
CREATE INDEX document_verif_timestamp_2 ON es_document_verification (timestamp_last_updated);

CREATE TABLE es_document_data (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activation_id VARCHAR(36) NOT NULL,
    identity_verification_id VARCHAR(36) NOT NULL,
    filename VARCHAR(256) NOT NULL,
    data BYTEA NOT NULL,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification (id)
);

CREATE INDEX document_data_activation ON es_document_data (activation_id);
CREATE INDEX document_data_timestamp ON es_document_data (timestamp_created);

CREATE TABLE es_document_result (
    id BIGINT NOT NULL PRIMARY KEY,
    document_verification_id VARCHAR(36) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    reject_reason TEXT,
    reject_origin VARCHAR(256),
    verification_result TEXT,
    error_detail TEXT,
    error_origin VARCHAR(256),
    extracted_data TEXT,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_verification_id) REFERENCES es_document_verification (id)
);

-- PostgreSQL does not create indexes on foreign keys automatically
CREATE INDEX document_verif_result ON es_document_result (document_verification_id);

CREATE SEQUENCE es_sca_result_seq INCREMENT BY 50 START WITH 1;

CREATE TABLE es_sca_result
(
    id                       BIGINT      NOT NULL PRIMARY KEY,
    identity_verification_id VARCHAR(36) NOT NULL,
    process_id               VARCHAR(36) NOT NULL,
    presence_check_result    VARCHAR(32),
    otp_verification_result  VARCHAR(32),
    sca_result               VARCHAR(32),
    timestamp_created        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated   TIMESTAMP,
    FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification (id),
    FOREIGN KEY (process_id) REFERENCES es_onboarding_process (id)
);

CREATE INDEX identity_verification_id ON es_sca_result (identity_verification_id);
CREATE INDEX process_id ON es_sca_result (process_id);

-- Scheduler lock table - https://github.com/lukas-krecan/ShedLock#configure-lockprovider
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Create audit log table - https://github.com/wultra/java-core#wultra-auditing-library
CREATE TABLE IF NOT EXISTS audit_log (
    audit_log_id       VARCHAR(36) PRIMARY KEY,
    application_name   VARCHAR(256) NOT NULL,
    audit_level        VARCHAR(32) NOT NULL,
    audit_type         VARCHAR(256),
    timestamp_created  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message            TEXT NOT NULL,
    exception_message  TEXT,
    stack_trace        TEXT,
    param              TEXT,
    calling_class      VARCHAR(256) NOT NULL,
    thread_name        VARCHAR(256) NOT NULL,
    version            VARCHAR(256),
    build_time         TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_param (
    audit_log_id       VARCHAR(36),
    timestamp_created  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    param_key          VARCHAR(256),
    param_value        VARCHAR(4000)
);

CREATE INDEX IF NOT EXISTS audit_log_timestamp ON audit_log (timestamp_created);
CREATE INDEX IF NOT EXISTS audit_log_application ON audit_log (application_name);
CREATE INDEX IF NOT EXISTS audit_log_level ON audit_log (audit_level);
CREATE INDEX IF NOT EXISTS audit_log_type ON audit_log (audit_type);
CREATE INDEX IF NOT EXISTS audit_param_log ON audit_param (audit_log_id);
CREATE INDEX IF NOT EXISTS audit_param_timestamp ON audit_param (timestamp_created);
CREATE INDEX IF NOT EXISTS audit_param_key ON audit_param (param_key);
CREATE INDEX IF NOT EXISTS audit_param_value ON audit_param (param_value);

CREATE INDEX IF NOT EXISTS onboarding_process_activation_id ON es_onboarding_process(activation_id);

-- Changeset enrollment-server-onboarding/1.7.x/20250513-add-tag-1.7.0.xml::1::Lubos Racansky
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
