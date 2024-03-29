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

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
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
 * Entity representing an onboarding OTP code.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "es_onboarding_otp")
public class OnboardingOtpEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -5626187612981527923L;

    public static final String ERROR_CANCELED = "canceledOtp";
    public static final String ERROR_RESEND = "resendOtp";
    public static final String ERROR_EXPIRED = "expiredOtp";
    public static final String ERROR_MAX_FAILED_ATTEMPTS = "maxFailedAttemptsOtp";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "process_id", referencedColumnName = "id", nullable = false)
    private OnboardingProcessEntity process;

    /**
     * Not-null only for {@link OtpType#USER_VERIFICATION} when limit is needed to count not over the whole process.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identity_verification_id", referencedColumnName = "id", updatable = false)
    private IdentityVerificationEntity identityVerification;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OtpStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OtpType type;

    @Column(name = "error_detail")
    private String errorDetail;

    @Column(name = "error_origin")
    @Enumerated(EnumType.STRING)
    private ErrorOrigin errorOrigin;

    @Column(name = "failed_attempts")
    private int failedAttempts;

    @Column(name = "total_attempts")
    private int totalAttempts;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Column(name = "timestamp_expiration", nullable = false)
    private Date timestampExpiration;

    @Column(name = "timestamp_last_updated")
    private Date timestampLastUpdated;

    @Column(name = "timestamp_verified")
    private Date timestampVerified;

    @Column(name = "timestamp_failed")
    private Date timestampFailed;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof final OnboardingOtpEntity that)) return false;
        return process.equals(that.process) && type.equals(that.type) && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(process, type, timestampCreated);
    }

    /**
     * @return true when the OTP has expired, false otherwise
     */
    @Transient
    public boolean hasExpired() {
        return timestampCreated.after(timestampExpiration);
    }

}
