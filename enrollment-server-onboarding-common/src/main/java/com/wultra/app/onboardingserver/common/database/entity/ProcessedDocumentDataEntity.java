package com.wultra.app.onboardingserver.common.database.entity;

/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity representing document data processed by provider.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "es_processed_document_data")
public class ProcessedDocumentDataEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 7685715667785423080L;

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "data", nullable = false, columnDefinition = "CLOB")
    private byte[] data;

    @Column(name = "data_type", nullable = false)
    private ProcessedDocumentDataType dataType;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

}
