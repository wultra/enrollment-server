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

CREATE SEQUENCE ES_OPERATION_TEMPLATE_SEQ MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20;

CREATE TABLE ES_OPERATION_TEMPLATE (
    ID NUMBER(19) NOT NULL PRIMARY KEY,
    PLACEHOLDER VARCHAR2(255 CHAR) NOT NULL,
    LANGUAGE VARCHAR2(8 CHAR) NOT NULL,
    TITLE VARCHAR2(255 CHAR) NOT NULL,
    MESSAGE CLOB NOT NULL,
    ATTRIBUTES CLOB,
    UI CLOB
);

CREATE UNIQUE INDEX ES_OPERATION_TEMPLATE_PLACEHOLDER ON ES_OPERATION_TEMPLATE(PLACEHOLDER, LANGUAGE);

-- Scheduler lock table - https://github.com/lukas-krecan/ShedLock#configure-lockprovider
BEGIN EXECUTE IMMEDIATE 'CREATE TABLE shedlock (
    name VARCHAR2(64 CHAR) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL,
    locked_by VARCHAR2(255 CHAR) NOT NULL,
    PRIMARY KEY (name))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

-- Create audit log table - https://github.com/wultra/java-core#wultra-auditing-library
BEGIN EXECUTE IMMEDIATE 'CREATE TABLE audit_log (
    audit_log_id       VARCHAR2(36 CHAR) PRIMARY KEY,
    application_name   VARCHAR2(256 CHAR) NOT NULL,
    audit_level        VARCHAR2(32 CHAR) NOT NULL,
    audit_type         VARCHAR2(256 CHAR),
    timestamp_created  TIMESTAMP,
    message            CLOB NOT NULL,
    exception_message  CLOB,
    stack_trace        CLOB,
    param              CLOB,
    calling_class      VARCHAR2(256 CHAR) NOT NULL,
    thread_name        VARCHAR2(256 CHAR) NOT NULL,
    version            VARCHAR2(256 CHAR),
    build_time         TIMESTAMP)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE TABLE audit_param (
    audit_log_id       VARCHAR2(36 CHAR),
    timestamp_created  TIMESTAMP,
    param_key          VARCHAR2(256 CHAR),
    param_value        VARCHAR2(4000 CHAR))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX audit_log_timestamp ON audit_log (timestamp_created)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX audit_log_application ON audit_log (application_name)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX audit_log_level ON audit_log (audit_level)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX audit_log_type ON audit_log (audit_type)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX audit_param_log ON audit_param (audit_log_id)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX audit_param_timestamp ON audit_param (timestamp_created)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX audit_param_key ON audit_param (param_key)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX audit_param_value ON audit_param (param_value)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

-- Changeset enrollment-server/1.8.x/20240620-add-resultTexts.xml::1::Lubos Racansky
-- Add result_texts column
ALTER TABLE es_operation_template ADD result_texts CLOB;

-- Changeset enrollment-server/2.1.x/20260330-audit-subject-id.xml::1::Pavel Sindelar
-- Add subject_id column to audit_log table
ALTER TABLE audit_log ADD subject_id VARCHAR2(256);

-- Changeset enrollment-server/2.1.x/20260330-audit-subject-id.xml::2::Pavel Sindelar
-- Create a new index on audit_log(subject_id)
CREATE INDEX audit_log_subject_id_idx ON audit_log(subject_id);
