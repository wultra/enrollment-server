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

--
--  Create sequences. Maximum value for PostgreSQL is 9223372036854775807.
--- See: https://www.postgresql.org/docs/9.6/sql-createsequence.html
--
CREATE SEQUENCE "es_operation_template_seq" MINVALUE 1 MAXVALUE 9223372036854775807 INCREMENT BY 1 START WITH 1 CACHE 20;

CREATE TABLE es_operation_template (
    id BIGINT NOT NULL PRIMARY KEY,
    placeholder VARCHAR(255) NOT NULL,
    language VARCHAR(8) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    attributes TEXT,
    ui TEXT
);

CREATE UNIQUE INDEX es_operation_template_placeholder ON es_operation_template(placeholder, language);

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

-- Changeset enrollment-server/1.8.x/20240620-add-resultTexts.xml::1::Lubos Racansky
-- Add result_texts column
ALTER TABLE es_operation_template ADD result_texts TEXT;

-- Changeset enrollment-server/2.1.x/20260330-audit-subject-id.xml::1::Pavel Sindelar
-- Add subject_id column to audit_log table
ALTER TABLE audit_log ADD subject_id VARCHAR(256);

-- Changeset enrollment-server/2.1.x/20260330-audit-subject-id.xml::2::Pavel Sindelar
-- Create a new index on audit_log(subject_id)
CREATE INDEX audit_log_subject_id_idx ON audit_log(subject_id);
