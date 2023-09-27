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

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Entity representing document data.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "es_document_data")
public class DocumentDataEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 7685715667785423079L;

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "activation_id", nullable = false)
    private String activationId;

    /**
     * Identifier of the related identity verification entity
     */
    @ManyToOne
    @JoinColumn(name = "identity_verification_id", referencedColumnName = "id", updatable = false, nullable = false)
    private IdentityVerificationEntity identityVerification;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof final DocumentDataEntity that)) return false;
        return filename.equals(that.filename) && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, timestampCreated);
    }

}

