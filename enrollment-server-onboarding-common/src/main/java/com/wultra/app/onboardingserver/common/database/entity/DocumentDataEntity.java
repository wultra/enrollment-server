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

package com.wultra.app.onboardingserver.common.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Entity representing document data uploaded from mobile SDK.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "es_document_data")
public class DocumentDataEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 7685715667785423079L;

    /***
     * Document data ID referenced as {@link DocumentVerificationEntity#uploadId}.
     */
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    /**
     * Document binary data.
     */
    @Column(name = "data", nullable = false, columnDefinition = "CLOB")
    private byte[] data;

    /**
     * Timestamp when the document data was created.
     */
    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    /**
     * ID of linked {@link DocumentVerificationEntity}.
     *
     * @implNote Nullable for backward compatibility. New records should be non-null.
     */
    @Column(name = "document_verification_id")
    @NotNull
    private String documentVerificationId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        final var oEffectiveClass = o instanceof HibernateProxy oProxy ?
                oProxy.getHibernateLazyInitializer().getPersistentClass() :
                o.getClass();
        final var thisEffectiveClass = this instanceof HibernateProxy thisProxy ?
                thisProxy.getHibernateLazyInitializer().getPersistentClass() :
                this.getClass();

        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }

        final var entity = (DocumentDataEntity) o;
        return getId() != null && Objects.equals(getId(), entity.getId());
    }

    @Override
    public int hashCode() {
        return this instanceof HibernateProxy proxy ?
                proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() :
                getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DocumentDataEntity(" +
                "id=" + id +
                ", data=" + (data != null ? "byte[" + data.length + "]" : null) +
                ", timestampCreated=" + timestampCreated + ")";
    }

}

