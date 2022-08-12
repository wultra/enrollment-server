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
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    placeholder VARCHAR(255) NOT NULL,
    language VARCHAR(8) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    attributes TEXT
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE UNIQUE INDEX es_operation_template_placeholder ON es_operation_template(placeholder, language);

CREATE TABLE es_onboarding_process (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    identification_data TEXT NOT NULL,
    user_id VARCHAR(256),
    activation_id VARCHAR(36),
    status VARCHAR(32) NOT NULL,
    error_detail VARCHAR(256),
    error_score INTEGER NOT NULL DEFAULT 0,
    timestamp_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated DATETIME,
    timestamp_finished DATETIME
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX onboarding_process_status ON es_onboarding_process (status);
CREATE INDEX onboarding_process_timestamp_1 ON es_onboarding_process (timestamp_created);
CREATE INDEX onboarding_process_timestamp_2 ON es_onboarding_process (timestamp_last_updated);

CREATE TABLE es_onboarding_otp (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    process_id VARCHAR(36) NOT NULL,
    otp_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    error_detail VARCHAR(256),
    failed_attempts INTEGER,
    timestamp_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_expiration DATETIME NOT NULL,
    timestamp_last_updated DATETIME,
    timestamp_verified DATETIME,
    FOREIGN KEY (process_id) REFERENCES es_onboarding_process (id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- MySQL creates indexes on foreign keys automatically
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
    error_detail VARCHAR(256),
    session_info TEXT,
    timestamp_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated DATETIME,
    FOREIGN KEY (process_id) REFERENCES es_onboarding_process (id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX identity_verif_activation ON es_identity_verification (activation_id);
CREATE INDEX identity_verif_user ON es_identity_verification (user_id);
CREATE INDEX identity_verif_status ON es_identity_verification (status);
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
    error_detail VARCHAR(256),
    original_document_id VARCHAR(36),
    used_for_verification TINYINT DEFAULT 0,
    timestamp_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_uploaded DATETIME,
    timestamp_verified DATETIME,
    timestamp_disposed DATETIME,
    timestamp_last_updated DATETIME,
    FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification (id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- MySQL creates indexes on foreign keys automatically
CREATE INDEX onboarding_verif_activation ON es_document_verification (activation_id);
CREATE INDEX onboarding_verif_status ON es_document_verification (status);
CREATE INDEX onboarding_verif_timestamp_1 ON es_document_verification (timestamp_created);
CREATE INDEX onboarding_verif_timestamp_2 ON es_document_verification (timestamp_last_updated);

CREATE TABLE es_document_data (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    activation_id VARCHAR(36) NOT NULL,
    identity_verification_id VARCHAR(36) NOT NULL,
    filename VARCHAR(256) NOT NULL,
    data BLOB NOT NULL,
    timestamp_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification (id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX document_data_activation ON es_document_data (activation_id);
CREATE INDEX document_data_timestamp ON es_document_data (timestamp_created);

CREATE TABLE es_document_result (
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    document_verification_id VARCHAR(36) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    reject_reason TEXT,
    verification_result TEXT,
    error_detail TEXT,
    extracted_data TEXT,
    timestamp_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_verification_id) REFERENCES es_document_verification (id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- MySQL creates indexes on foreign keys automatically

-- Scheduler lock table - https://github.com/lukas-krecan/ShedLock#configure-lockprovider
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
