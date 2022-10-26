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

-- Scheduler lock table - https://github.com/lukas-krecan/ShedLock#configure-lockprovider
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Create audit log table - https://github.com/wultra/lime-java-core#wultra-auditing-library
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

CREATE INDEX IF NOT EXISTS audit_log_timestamp ON audit_log (timestamp_created);
CREATE INDEX IF NOT EXISTS audit_log_application ON audit_log (application_name);
CREATE INDEX IF NOT EXISTS audit_log_level ON audit_log (audit_level);
CREATE INDEX IF NOT EXISTS audit_log_type ON audit_log (audit_type);
CREATE INDEX IF NOT EXISTS audit_param_log ON audit_param (audit_log_id);
CREATE INDEX IF NOT EXISTS audit_param_timestamp ON audit_param (timestamp_created);
CREATE INDEX IF NOT EXISTS audit_param_key ON audit_param (param_key);
CREATE INDEX IF NOT EXISTS audit_param_value ON audit_param (param_value);