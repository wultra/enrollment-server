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
