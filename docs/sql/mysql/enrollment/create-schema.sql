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
 *
 */

CREATE TABLE es_operation_template (
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    placeholder VARCHAR(255) NOT NULL,
    language VARCHAR(8) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    attributes TEXT,
    ui TEXT
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE UNIQUE INDEX es_operation_template_placeholder ON es_operation_template(placeholder, language);
