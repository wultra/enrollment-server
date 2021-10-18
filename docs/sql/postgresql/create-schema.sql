/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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

CREATE TABLE es_operation_template (
    id BIGINT NOT NULL PRIMARY KEY,
    placeholder VARCHAR(255) NOT NULL,
    language VARCHAR(8) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    attributes TEXT
);

CREATE UNIQUE INDEX es_operation_template_placeholder ON es_operation_template(placeholder, language);

CREATE TABLE es_onboarding_process (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    identification_data TEXT NOT NULL,
    user_id VARCHAR(256) NOT NULL,
    activation_id VARCHAR(36),
    status VARCHAR(32) NOT NULL,
    error_detail VARCHAR(256),
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated TIMESTAMP,
    timestamp_finished TIMESTAMP
);

CREATE INDEX document_process_status ON es_onboarding_process (status);
CREATE INDEX document_process_timestamp_1 ON es_onboarding_process (timestamp_created);
CREATE INDEX document_process_timestamp_2 ON es_onboarding_process (timestamp_last_updated);

CREATE TABLE es_onboarding_otp (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    process_id VARCHAR(36) NOT NULL,
    otp_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_detail VARCHAR(256),
    failed_attempts INTEGER,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated TIMESTAMP,
    timestamp_verified TIMESTAMP,
    FOREIGN KEY (process_id) REFERENCES es_onboarding_process (id)
);

CREATE INDEX document_otp_status ON es_onboarding_otp (status);
CREATE INDEX document_otp_timestamp_1 ON es_onboarding_otp (timestamp_created);
CREATE INDEX document_otp_timestamp_2 ON es_onboarding_otp (timestamp_last_updated);

CREATE TABLE es_document_verification (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activation_id VARCHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    side VARCHAR(5),
    other_side_id VARCHAR(36),
    status VARCHAR(32) NOT NULL,
    filename VARCHAR(256) NOT NULL,
    upload_id VARCHAR(36),
    verification_id VARCHAR(36),
    verification_score INTEGER,
    reject_reason VARCHAR(256),
    error_detail VARCHAR(256),
    original_document_id VARCHAR(36),
    used_for_verification BOOLEAN DEFAULT FALSE,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_uploaded TIMESTAMP,
    timestamp_verified TIMESTAMP,
    timestamp_disposed TIMESTAMP,
    timestamp_last_updated TIMESTAMP
);

CREATE INDEX document_verif_activation ON es_document_verification (activation_id);
CREATE INDEX document_verif_status ON es_document_verification (status);
CREATE INDEX document_verif_timestamp_1 ON es_document_verification (timestamp_created);
CREATE INDEX document_verif_timestamp_2 ON es_document_verification (timestamp_last_updated);

CREATE TABLE es_document_data (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activation_id VARCHAR(36) NOT NULL,
    filename VARCHAR(256) NOT NULL,
    data BYTEA NOT NULL,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX document_data_activation ON es_document_data (activation_id);
CREATE INDEX document_data_timestamp ON es_document_data (timestamp_created);

CREATE TABLE es_document_result (
    id BIGINT NOT NULL PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    data TEXT,
    verification_result TEXT,
    errors TEXT,
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES es_document_verification (id)
);

CREATE INDEX document_result ON es_document_result (document_id);

CREATE TABLE es_identity_verification (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reject_reason VARCHAR(256),
    error_detail VARCHAR(256),
    timestamp_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated TIMESTAMP
);

CREATE INDEX identity_verif_activation ON es_identity_verification (activation_id);
CREATE INDEX identity_verif_user ON es_identity_verification (user_id);
CREATE INDEX identity_verif_status ON es_identity_verification (status);
CREATE INDEX identity_verif_timestamp_1 ON es_identity_verification (timestamp_created);
CREATE INDEX identity_verif_timestamp_2 ON es_identity_verification (timestamp_last_updated);

CREATE TABLE es_identity_document (
    identity_verification_id VARCHAR(36) NOT NULL,
    document_verification_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (identity_verification_id, document_verification_id),
    FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification (id),
    FOREIGN KEY (document_verification_id) REFERENCES es_document_verification (id)
);
