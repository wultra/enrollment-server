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

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity representing binding of identity verification records with document verification records.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "es_identity_document")
public class IdentityDocumentEntity implements Serializable {

    private static final long serialVersionUID = -3766573464682729277L;

    @EmbeddedId
    private IdentityDocumentKey id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityDocumentEntity)) return false;
        IdentityDocumentEntity that = (IdentityDocumentEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityDocumentKey implements Serializable {

        private static final long serialVersionUID = -5100158130822709117L;

        @Column(name = "identity_id", nullable = false)
        private String identityId;

        @Column(name = "document_id", nullable = false)
        private String documentId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdentityDocumentKey)) return false;
            IdentityDocumentKey that = (IdentityDocumentKey) o;
            return identityId.equals(that.identityId) && documentId.equals(that.documentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identityId, documentId);
        }
    }
}