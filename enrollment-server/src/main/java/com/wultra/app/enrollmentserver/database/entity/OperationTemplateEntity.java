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

import lombok.*;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity representing an operation template.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "es_operation_template")
public class OperationTemplateEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 5914420785283118800L;

    @Id
    @SequenceGenerator(name = "es_operation_template", sequenceName = "es_operation_template_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "es_operation_template")
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Operation type at PowerAuth server.
     */
    @Column(name = "placeholder", nullable = false)
    private String placeholder;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "ui")
    private String ui;

    @Column(name = "attributes")
    private String attributes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof final OperationTemplateEntity that)) return false;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(language, that.language)
                && Objects.equals(title, that.title)
                && Objects.equals(message, that.message)
                && Objects.equals(ui, that.ui)
                && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeholder, language, title, message, ui, attributes);
    }
}
