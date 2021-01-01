/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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

package com.wultra.app.enrollmentserver.database.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Entity representing an operation template.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
@Entity
@Table(name = "es_operation_template")
public class OperationTemplate implements Serializable {

    @Id
    @SequenceGenerator(name = "es_operation_template", sequenceName = "es_operation_template_seq")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "es_operation_template")
    @Column(name = "id")
    private Long id;

    @Column(name = "placeholder")
    private String placeholder;

    @Column(name = "language")
    private String language;

    @Column(name = "title")
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "attributes")
    private String attributes;

}
