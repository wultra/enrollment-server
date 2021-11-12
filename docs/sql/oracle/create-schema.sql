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

CREATE TABLE ES_OPERATION_TEMPLATE (
    ID NUMBER(19) NOT NULL PRIMARY KEY,
    PLACEHOLDER VARCHAR2(255 CHAR) NOT NULL,
    LANGUAGE VARCHAR2(8 CHAR) NOT NULL,
    TITLE VARCHAR2(255 CHAR) NOT NULL,
    MESSAGE BLOB NOT NULL,
    ATTRIBUTES BLOB
);

CREATE UNIQUE INDEX ES_OPERATION_TEMPLATE_PLACEHOLDER ON ES_OPERATION_TEMPLATE(PLACEHOLDER, LANGUAGE);

CREATE TABLE ES_ONBOARDING_PROCESS (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    IDENTIFICATION_DATA CLOB NOT NULL,
    USER_ID VARCHAR2(256 CHAR) NOT NULL,
    ACTIVATION_ID VARCHAR2(36 CHAR),
    STATUS VARCHAR2(32 CHAR) NOT NULL,
    ERROR_DETAIL VARCHAR2(256 CHAR),
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    TIMESTAMP_LAST_UPDATED TIMESTAMP(6),
    TIMESTAMP_FINISHED TIMESTAMP(6)
);

CREATE INDEX DOCUMENT_PROCESS_STATUS ON ES_ONBOARDING_PROCESS (STATUS);
CREATE INDEX DOCUMENT_PROCESS_TIMESTAMP_1 ON ES_ONBOARDING_PROCESS (TIMESTAMP_CREATED);
CREATE INDEX DOCUMENT_PROCESS_TIMESTAMP_2 ON ES_ONBOARDING_PROCESS (TIMESTAMP_LAST_UPDATED);

CREATE TABLE ES_ONBOARDING_OTP (
    ID VARCHAR(36) NOT NULL PRIMARY KEY,
    PROCESS_ID VARCHAR(36) NOT NULL,
    OTP_CODE VARCHAR(32) NOT NULL,
    STATUS VARCHAR(32) NOT NULL,
    ERROR_DETAIL VARCHAR(256),
    FAILED_ATTEMPTS INTEGER,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    TIMESTAMP_LAST_UPDATED TIMESTAMP(6),
    TIMESTAMP_VERIFIED TIMESTAMP(6),
    FOREIGN KEY (PROCESS_ID) REFERENCES ES_ONBOARDING_PROCESS (ID)
);

CREATE INDEX DOCUMENT_OTP_STATUS ON ES_ONBOARDING_OTP (STATUS);
CREATE INDEX DOCUMENT_OTP_TIMESTAMP_1 ON ES_ONBOARDING_OTP (TIMESTAMP_CREATED);
CREATE INDEX DOCUMENT_OTP_TIMESTAMP_2 ON ES_ONBOARDING_OTP (TIMESTAMP_LAST_UPDATED);

CREATE TABLE ES_DOCUMENT_VERIFICATION (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    ACTIVATION_ID VARCHAR2(36 CHAR) NOT NULL,
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
    REJECT_REASON VARCHAR2(256 CHAR),
    ERROR_DETAIL VARCHAR2(256 CHAR),
    ORIGINAL_DOCUMENT_ID VARCHAR2(36 CHAR),
    USED_FOR_VERIFICATION NUMBER(1) DEFAULT 0,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    TIMESTAMP_UPLOADED TIMESTAMP(6),
    TIMESTAMP_VERIFIED TIMESTAMP(6),
    TIMESTAMP_DISPOSED TIMESTAMP(6),
    TIMESTAMP_LAST_UPDATED TIMESTAMP(6)
);

CREATE INDEX DOCUMENT_VERIF_ACTIVATION ON ES_DOCUMENT_VERIFICATION (ACTIVATION_ID);
CREATE INDEX DOCUMENT_VERIF_STATUS ON ES_DOCUMENT_VERIFICATION (STATUS);
CREATE INDEX DOCUMENT_VERIF_TIMESTAMP_1 ON ES_DOCUMENT_VERIFICATION (TIMESTAMP_CREATED);
CREATE INDEX DOCUMENT_VERIF_TIMESTAMP_2 ON ES_DOCUMENT_VERIFICATION (TIMESTAMP_LAST_UPDATED);

CREATE TABLE ES_DOCUMENT_DATA (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    ACTIVATION_ID VARCHAR2(36 CHAR) NOT NULL,
    FILENAME VARCHAR2(256 CHAR) NOT NULL,
    DATA BLOB NOT NULL,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL
);

CREATE INDEX DOCUMENT_DATA_ACTIVATION ON ES_DOCUMENT_DATA (ACTIVATION_ID);
CREATE INDEX DOCUMENT_DATA_TIMESTAMP ON ES_DOCUMENT_DATA (TIMESTAMP_CREATED);

CREATE TABLE ES_DOCUMENT_RESULT (
    ID NUMBER(19) NOT NULL PRIMARY KEY,
    DOCUMENT_VERIFICATION_ID VARCHAR2(36 CHAR) NOT NULL,
    PHASE VARCHAR2(32 CHAR) NOT NULL,
    REJECT_REASON CLOB,
    VERIFICATION_RESULT CLOB,
    ERROR_DETAIL CLOB,
    EXTRACTED_DATA CLOB,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    FOREIGN KEY (DOCUMENT_VERIFICATION_ID) REFERENCES ES_DOCUMENT_VERIFICATION (ID)
);

CREATE INDEX DOCUMENT_VERIF_RESULT ON ES_DOCUMENT_RESULT (DOCUMENT_VERIFICATION_ID);

CREATE TABLE ES_IDENTITY_VERIFICATION (
    ID VARCHAR2(36 CHAR) NOT NULL PRIMARY KEY,
    ACTIVATION_ID VARCHAR2(36 CHAR) NOT NULL,
    USER_ID VARCHAR2(256 CHAR) NOT NULL,
    STATUS VARCHAR2(32 CHAR) NOT NULL,
    PHASE VARCHAR2(32 CHAR) NOT NULL,
    REJECT_REASON VARCHAR2(256 CHAR),
    ERROR_DETAIL VARCHAR2(256 CHAR),
    SESSION_INFO CLOB,
    TIMESTAMP_CREATED TIMESTAMP(6) NOT NULL,
    TIMESTAMP_LAST_UPDATED TIMESTAMP(6)
);

CREATE INDEX IDENTITY_VERIF_ACTIVATION ON ES_IDENTITY_VERIFICATION (ACTIVATION_ID);
CREATE INDEX IDENTITY_VERIF_USER ON ES_IDENTITY_VERIFICATION (USER_ID);
CREATE INDEX IDENTITY_VERIF_STATUS ON ES_IDENTITY_VERIFICATION (STATUS);
CREATE INDEX IDENTITY_VERIF_TIMESTAMP_1 ON ES_IDENTITY_VERIFICATION (TIMESTAMP_CREATED);
CREATE INDEX IDENTITY_VERIF_TIMESTAMP_2 ON ES_IDENTITY_VERIFICATION (TIMESTAMP_LAST_UPDATED);

CREATE TABLE ES_IDENTITY_DOCUMENT (
    IDENTITY_VERIFICATION_ID VARCHAR2(36 CHAR) NOT NULL,
    DOCUMENT_VERIFICATION_ID VARCHAR2(36 CHAR) NOT NULL,
    PRIMARY KEY (IDENTITY_VERIFICATION_ID, DOCUMENT_VERIFICATION_ID),
    FOREIGN KEY (IDENTITY_VERIFICATION_ID) REFERENCES ES_IDENTITY_VERIFICATION (ID),
    FOREIGN KEY (DOCUMENT_VERIFICATION_ID) REFERENCES ES_DOCUMENT_VERIFICATION (ID)
);
