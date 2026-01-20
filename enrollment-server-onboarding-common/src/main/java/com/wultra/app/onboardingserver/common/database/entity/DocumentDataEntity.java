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
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "data", nullable = false, columnDefinition = "CLOB")
    private byte[] data;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @OneToOne(
            mappedBy = "documentData",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private DocumentVerificationEntity documentVerification;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof final DocumentDataEntity that)) return false;
        return id.equals(that.id) && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestampCreated);
    }

}

