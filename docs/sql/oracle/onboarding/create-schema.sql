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

CREATE SEQUENCE ES_DOCUMENT_RESULT_SEQ MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 10 START WITH 1 CACHE 20;

CREATE TABLE ES_ONBOARDING_PROCESS (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    IDENTIFICATION_DATA VARCHAR2(1024 CHAR) NOT NULL,
    USER_ID VARCHAR2(256 CHAR),
    ACTIVATION_ID VARCHAR2(36 CHAR),
    STATUS VARCHAR2(32 CHAR) NOT NULL,
    ACTIVATION_REMOVED NUMBER(1) DEFAULT 0,
    ERROR_DETAIL VARCHAR2(256 CHAR),
    ERROR_ORIGIN VARCHAR2(256 CHAR),
    ERROR_SCORE INTEGER DEFAULT 0 NOT NULL,
    CUSTOM_DATA VARCHAR2(1024 CHAR) NOT NULL,
    FDS_DATA CLOB,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    TIMESTAMP_LAST_UPDATED TIMESTAMP(6),
    TIMESTAMP_FINISHED TIMESTAMP(6),
    TIMESTAMP_FAILED TIMESTAMP(6)
);

CREATE INDEX ONBOARDING_PROCESS_STATUS ON ES_ONBOARDING_PROCESS (STATUS);
CREATE INDEX ONBOARDING_PROCESS_IDENTIF_DATA ON ES_ONBOARDING_PROCESS (IDENTIFICATION_DATA);
CREATE INDEX ONBOARDING_PROCESS_TIMESTAMP_1 ON ES_ONBOARDING_PROCESS (TIMESTAMP_CREATED);
CREATE INDEX ONBOARDING_PROCESS_TIMESTAMP_2 ON ES_ONBOARDING_PROCESS (TIMESTAMP_LAST_UPDATED);

CREATE TABLE ES_ONBOARDING_OTP (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    PROCESS_ID VARCHAR2(36 CHAR) NOT NULL,
    IDENTITY_VERIFICATION_ID VARCHAR2(36 CHAR),
    OTP_CODE VARCHAR2(32 CHAR) NOT NULL,
    STATUS VARCHAR2(32 CHAR) NOT NULL,
    TYPE VARCHAR2(32 CHAR) NOT NULL,
    ERROR_DETAIL VARCHAR2(256 CHAR),
    ERROR_ORIGIN VARCHAR2(256 CHAR),
    FAILED_ATTEMPTS INTEGER,
    TOTAL_ATTEMPTS INTEGER DEFAULT 0,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    TIMESTAMP_EXPIRATION TIMESTAMP(6) NOT NULL,
    TIMESTAMP_LAST_UPDATED TIMESTAMP(6),
    TIMESTAMP_VERIFIED TIMESTAMP(6),
    TIMESTAMP_FAILED TIMESTAMP(6),
    FOREIGN KEY (PROCESS_ID) REFERENCES ES_ONBOARDING_PROCESS (ID),
    FOREIGN KEY (IDENTITY_VERIFICATION_ID) REFERENCES ES_IDENTITY_VERIFICATION (ID)
);

-- Oracle does not create indexes on foreign keys automatically
CREATE INDEX ONBOARDING_PROCESS ON ES_ONBOARDING_OTP (PROCESS_ID);
CREATE INDEX ONBOARDING_OTP_STATUS ON ES_ONBOARDING_OTP (STATUS);
CREATE INDEX ONBOARDING_OTP_TIMESTAMP_1 ON ES_ONBOARDING_OTP (TIMESTAMP_CREATED);
CREATE INDEX ONBOARDING_OTP_TIMESTAMP_2 ON ES_ONBOARDING_OTP (TIMESTAMP_LAST_UPDATED);

CREATE TABLE ES_IDENTITY_VERIFICATION (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    ACTIVATION_ID VARCHAR2(36 CHAR) NOT NULL,
    USER_ID VARCHAR2(256 CHAR) NOT NULL,
    PROCESS_ID VARCHAR2(36 CHAR) NOT NULL,
    STATUS VARCHAR2(32 CHAR) NOT NULL,
    PHASE VARCHAR2(32 CHAR) NOT NULL,
    REJECT_REASON CLOB,
    REJECT_ORIGIN VARCHAR2(256 CHAR),
    ERROR_DETAIL VARCHAR2(256 CHAR),
    ERROR_ORIGIN VARCHAR2(256 CHAR),
    SESSION_INFO CLOB,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    TIMESTAMP_LAST_UPDATED TIMESTAMP(6),
    TIMESTAMP_FINISHED TIMESTAMP(6),
    TIMESTAMP_FAILED TIMESTAMP(6),
    FOREIGN KEY (PROCESS_ID) REFERENCES ES_ONBOARDING_PROCESS (ID)
);

CREATE INDEX IDENTITY_VERIF_ACTIVATION ON ES_IDENTITY_VERIFICATION (ACTIVATION_ID);
CREATE INDEX IDENTITY_VERIF_USER ON ES_IDENTITY_VERIFICATION (USER_ID);
CREATE INDEX IDENTITY_VERIF_STATUS ON ES_IDENTITY_VERIFICATION (STATUS);
CREATE INDEX IDENTITY_VERIF_PHASE ON ES_IDENTITY_VERIFICATION (PHASE);
CREATE INDEX IDENTITY_VERIF_TIMESTAMP_1 ON ES_IDENTITY_VERIFICATION (TIMESTAMP_CREATED);
CREATE INDEX IDENTITY_VERIF_TIMESTAMP_2 ON ES_IDENTITY_VERIFICATION (TIMESTAMP_LAST_UPDATED);

CREATE TABLE ES_DOCUMENT_VERIFICATION (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    ACTIVATION_ID VARCHAR2(36 CHAR) NOT NULL,
    IDENTITY_VERIFICATION_ID VARCHAR2(36 CHAR) NOT NULL,
    TYPE VARCHAR2(32 CHAR) NOT NULL,
    SIDE VARCHAR2(5 CHAR),
    OTHER_SIDE_ID VARCHAR2(36 CHAR),
    PROVIDER_NAME VARCHAR2(64 CHAR),
    STATUS VARCHAR2(32 CHAR) NOT NULL,
    FILENAME VARCHAR2(256 CHAR) NOT NULL,
    UPLOAD_ID VARCHAR2(36 CHAR),
    VERIFICATION_ID VARCHAR2(36 CHAR),
    PHOTO_ID VARCHAR2(256 CHAR),
    VERIFICATION_SCORE INTEGER,
    REJECT_REASON CLOB,
    REJECT_ORIGIN VARCHAR2(256 CHAR),
    ERROR_DETAIL VARCHAR2(256 CHAR),
    ERROR_ORIGIN VARCHAR2(256 CHAR),
    ORIGINAL_DOCUMENT_ID VARCHAR2(36 CHAR),
    USED_FOR_VERIFICATION NUMBER(1) DEFAULT 0,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    TIMESTAMP_UPLOADED TIMESTAMP(6),
    TIMESTAMP_VERIFIED TIMESTAMP(6),
    TIMESTAMP_DISPOSED TIMESTAMP(6),
    TIMESTAMP_LAST_UPDATED TIMESTAMP(6),
    FOREIGN KEY (IDENTITY_VERIFICATION_ID) REFERENCES ES_IDENTITY_VERIFICATION (ID)
);

-- Oracle does not create indexes on foreign keys automatically
CREATE INDEX DOCUMENT_IDENT_VERIF ON ES_DOCUMENT_VERIFICATION (IDENTITY_VERIFICATION_ID);
CREATE INDEX DOCUMENT_VERIF_ACTIVATION ON ES_DOCUMENT_VERIFICATION (ACTIVATION_ID);
CREATE INDEX DOCUMENT_VERIF_STATUS ON ES_DOCUMENT_VERIFICATION (STATUS);
CREATE INDEX DOCUMENT_VERIF_TIMESTAMP_1 ON ES_DOCUMENT_VERIFICATION (TIMESTAMP_CREATED);
CREATE INDEX DOCUMENT_VERIF_TIMESTAMP_2 ON ES_DOCUMENT_VERIFICATION (TIMESTAMP_LAST_UPDATED);

CREATE TABLE ES_DOCUMENT_DATA (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    ACTIVATION_ID VARCHAR2(36 CHAR) NOT NULL,
    IDENTITY_VERIFICATION_ID VARCHAR2(36 CHAR) NOT NULL,
    FILENAME VARCHAR2(256 CHAR) NOT NULL,
    DATA BLOB NOT NULL,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    FOREIGN KEY (IDENTITY_VERIFICATION_ID) REFERENCES ES_IDENTITY_VERIFICATION (ID)
);

CREATE INDEX DOCUMENT_DATA_ACTIVATION ON ES_DOCUMENT_DATA (ACTIVATION_ID);
CREATE INDEX DOCUMENT_DATA_TIMESTAMP ON ES_DOCUMENT_DATA (TIMESTAMP_CREATED);

CREATE TABLE ES_DOCUMENT_RESULT (
    ID NUMBER(19) NOT NULL PRIMARY KEY,
    DOCUMENT_VERIFICATION_ID VARCHAR2(36 CHAR) NOT NULL,
    PHASE VARCHAR2(32 CHAR) NOT NULL,
    REJECT_REASON CLOB,
    REJECT_ORIGIN VARCHAR2(256 CHAR),
    VERIFICATION_RESULT CLOB,
    ERROR_DETAIL CLOB,
    ERROR_ORIGIN VARCHAR2(256 CHAR),
    EXTRACTED_DATA CLOB,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    FOREIGN KEY (DOCUMENT_VERIFICATION_ID) REFERENCES ES_DOCUMENT_VERIFICATION (ID)
);

-- Oracle does not create indexes on foreign keys automatically
CREATE INDEX DOCUMENT_VERIF_RESULT ON ES_DOCUMENT_RESULT (DOCUMENT_VERIFICATION_ID);

CREATE SEQUENCE ES_SCA_RESULT_SEQ INCREMENT BY 50 START WITH 1;

CREATE TABLE ES_SCA_RESULT
(
    ID                       NUMBER(19)        NOT NULL PRIMARY KEY,
    IDENTITY_VERIFICATION_ID VARCHAR2(36 CHAR) NOT NULL,
    PROCESS_ID               VARCHAR2(36 CHAR) NOT NULL,
    PRESENCE_CHECK_RESULT    VARCHAR2(32 CHAR),
    OTP_VERIFICATION_RESULT  VARCHAR2(32 CHAR),
    SCA_RESULT               VARCHAR2(32 CHAR),
    TIMESTAMP_CREATED        TIMESTAMP(6)      NOT NULL,
    TIMESTAMP_LAST_UPDATED   TIMESTAMP(6),
    FOREIGN KEY (IDENTITY_VERIFICATION_ID) REFERENCES ES_IDENTITY_VERIFICATION (ID),
    FOREIGN KEY (PROCESS_ID) REFERENCES ES_ONBOARDING_PROCESS (ID)
);

CREATE INDEX IDENTITY_VERIFICATION_ID ON ES_SCA_RESULT (IDENTITY_VERIFICATION_ID);
CREATE INDEX PROCESS_ID ON ES_SCA_RESULT (PROCESS_ID);

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

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX onboarding_process_activation_id ON es_onboarding_process(activation_id)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::1::Lubos Racansky
-- Create a new sequence es_onboarding_process_configuration_seq
CREATE SEQUENCE es_onboarding_process_configuration_seq START WITH 1 INCREMENT BY 50 CACHE 20;

-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::2::Lubos Racansky
-- Create a new table es_onboarding_process_configuration
CREATE TABLE es_onboarding_process_configuration (id NUMBER(38, 0) DEFAULT es_onboarding_process_configuration_seq.nextval NOT NULL, process_type VARCHAR2(255) NOT NULL, config CLOB NOT NULL, CONSTRAINT PK_ES_ONBOARDING_PROCESS_CONFIGURATION PRIMARY KEY (id), CONSTRAINT es_onboarding_process_configuration_process_type_uk UNIQUE (process_type));

-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::3::Lubos Racansky
-- Create a new column process_config_id to es_onboarding_process
ALTER TABLE es_onboarding_process ADD process_config_id NUMBER(38, 0);

ALTER TABLE es_onboarding_process ADD CONSTRAINT es_onboarding_process_process_config_id_fk FOREIGN KEY (process_config_id) REFERENCES es_onboarding_process_configuration (id);

-- Changeset enrollment-server-onboarding/2.0.x/20251219-process-target-activation-id.xml::1::Lubos Racansky
-- Create a new column target_activation_id to es_onboarding_process
ALTER TABLE es_onboarding_process ADD target_activation_id VARCHAR2(36);

-- Changeset enrollment-server-onboarding/2.0.x/20260116-add-processed-document-data-table.xml::1::Michal Rozehnal
-- Create a new table es_processed_document_data
CREATE TABLE es_processed_document_data (id VARCHAR2(36) NOT NULL, data BLOB NOT NULL, data_type VARCHAR2(32) NOT NULL, timestamp_created TIMESTAMP DEFAULT sysdate NOT NULL, CONSTRAINT PK_ES_PROCESSED_DOCUMENT_DATA PRIMARY KEY (id));

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
-- Add index for table es_document_data column timestamp_created
CREATE INDEX es_document_data_timestamp_created_idx ON es_document_data(timestamp_created);

-- Changeset enrollment-server-onboarding/2.0.x/20260116-add-processed-document-data-table.xml::7::Michal Rozehnal
-- Add index for table es_document_verification column upload_id
CREATE INDEX es_document_verification_upload_id_idx ON es_document_verification(upload_id);

-- Changeset enrollment-server-onboarding/2.0.x/20260126-selfie.xml::1::Lubos Racansky
-- Create a new sequence es_selfie_seq
CREATE SEQUENCE es_selfie_seq START WITH 1 INCREMENT BY 50 CACHE 20;

-- Changeset enrollment-server-onboarding/2.0.x/20260126-selfie.xml::2::Lubos Racansky
-- Create a new table es_selfie
CREATE TABLE es_selfie (id NUMBER(38, 0) DEFAULT es_selfie_seq.nextval NOT NULL, image BLOB, identity_verification_id VARCHAR2(36) NOT NULL, timestamp_created TIMESTAMP DEFAULT sysdate NOT NULL, CONSTRAINT PK_ES_SELFIE PRIMARY KEY (id), CONSTRAINT es_selfie_identity_verification_id_fk FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification(id));

-- Changeset enrollment-server-onboarding/2.0.x/20260126-selfie.xml::3::Lubos Racansky
-- Create a new index es_selfie_identity_verification_id_idx
CREATE INDEX es_selfie_identity_verification_id_idx ON es_selfie(identity_verification_id);

-- Changeset enrollment-server-onboarding/2.0.x/20260126-selfie.xml::4::Lubos Racansky
-- Create a new index es_selfie_timestamp_created_idx
CREATE INDEX es_selfie_timestamp_created_idx ON es_selfie(timestamp_created);

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::1::Lubos Racansky
-- Create a new table audit_log
CREATE TABLE audit_log (audit_log_id VARCHAR2(36) NOT NULL, application_name VARCHAR2(256) NOT NULL, audit_level VARCHAR2(32) NOT NULL, audit_type VARCHAR2(256), timestamp_created TIMESTAMP DEFAULT sysdate, message CLOB NOT NULL, exception_message CLOB, stack_trace CLOB, param CLOB, calling_class VARCHAR2(256) NOT NULL, thread_name VARCHAR2(256) NOT NULL, version VARCHAR2(256), build_time TIMESTAMP, CONSTRAINT PK_AUDIT_LOG PRIMARY KEY (audit_log_id));

-- Changeset enrollment-server/2.0.x/20260205-audit.xml::2::Lubos Racansky
-- Create a new table audit_log
CREATE TABLE audit_param (audit_log_id VARCHAR2(36), timestamp_created TIMESTAMP DEFAULT sysdate, param_key VARCHAR2(256), param_value VARCHAR2(4000));

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
ALTER TABLE es_processed_document_data ADD document_verification_id VARCHAR2(36);

ALTER TABLE es_processed_document_data ADD CONSTRAINT es_processed_document_data_document_verification_id_fk FOREIGN KEY (document_verification_id) REFERENCES es_document_verification (id);

-- Changeset enrollment-server-onboarding/2.0.x/20260309-processed-document-data-verification-id.xml::2::Michal Rozehnal
-- Creates a new index es_processed_document_data_document_verification_id_idx
CREATE INDEX es_processed_document_data_document_verification_id_idx ON es_processed_document_data(document_verification_id);

-- Changeset enrollment-server-onboarding/2.0.x/20260313-consent-accepted.xml::1::Michal Rozehnal
-- Creates a new column consent_accepted in es_onboarding_process
ALTER TABLE es_onboarding_process ADD consent_accepted BOOLEAN;

-- Changeset enrollment-server-onboarding/2.0.x/20251215-add-tag-2.0.0.xml::1::Lubos Racansky
-- Changeset enrollment-server-onboarding/2.1.x/20260330-audit-subject-id.xml::1::Pavel Sindelar
-- Add subject_id column to audit_log table
ALTER TABLE audit_log ADD subject_id VARCHAR2(256);

-- Changeset enrollment-server-onboarding/2.1.x/20260330-audit-subject-id.xml::2::Pavel Sindelar
-- Create a new index on audit_log(subject_id)
CREATE INDEX audit_log_subject_id_idx ON audit_log(subject_id);

-- Changeset enrollment-server-onboarding/2.0.x/20260402-document-country.xml::1::Lubos Racansky
-- Creates a new column country in es_document_verification
ALTER TABLE es_document_verification ADD country VARCHAR2(3);

-- Changeset enrollment-server-onboarding/2.1.x/20260423-document-result-timestamp-created-index.xml::1::Michal Rozehnal
-- Add index on timestamp_created column in es_document_result table
CREATE INDEX es_document_result_timestamp_created_idx ON es_document_result(timestamp_created);
