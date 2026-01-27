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
package com.wultra.app.onboardingserver.common.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Selfie entity stores an image retrieved during the presence check.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Getter
@Setter
@Entity
@Table(name = "es_selfie")
public class SelfieEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -7134510504507334015L;

    @SequenceGenerator(name = "es_selfie", sequenceName = "es_selfie_seq")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "es_selfie")
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Selfie image. Mind that it could be {@code null} if the presence check failed.
     */
    @Column(name = "image", columnDefinition = "CLOB")
    private byte[] image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identity_verification_id", nullable = false)
    private IdentityVerificationEntity identityVerification;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;
}
