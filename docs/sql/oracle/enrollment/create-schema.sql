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
