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

package com.wultra.app.onboardingserver.database.entity;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Entity representing identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Getter
@Setter
@ToString(of = {"id", "activationId", "phase"})
@NoArgsConstructor
@Entity
@Table(name = "es_identity_verification")
public class IdentityVerificationEntity implements Serializable {

    private static final long serialVersionUID = 6307591849271145826L;

    public static final String ERROR_MAX_FAILED_ATTEMPTS_DOCUMENT_UPLOAD = "maxFailedAttemptsDocumentUpload";
    public static final String ERROR_MAX_FAILED_ATTEMPTS_PRESENCE_CHECK = "maxFailedAttemptsPresenceCheck";

    public static final String ERROR_MAX_FAILED_ATTEMPTS_CLIENT_EVALUATION = "maxFailedAttemptsClientEvaluation";

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "activation_id", nullable = false)
    private String activationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "process_id", nullable = false)
    private String processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IdentityVerificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private IdentityVerificationPhase phase;

    @Column(name = "reject_reason")
    @Lob
    private String rejectReason;

    @Column(name = "error_detail")
    private String errorDetail;

    @Column(name = "session_info")
    @Lob
    private String sessionInfo;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Column(name = "timestamp_last_updated")
    private Date timestampLastUpdated;

    @OneToMany(mappedBy = "identityVerification", cascade = CascadeType.ALL)
    @OrderBy("timestampCreated")
    private Set<DocumentVerificationEntity> documentVerifications = new LinkedHashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityVerificationEntity)) return false;
        IdentityVerificationEntity that = (IdentityVerificationEntity) o;
        return activationId.equals(that.activationId) && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activationId, timestampCreated);
    }

    /**
     * Checks if the presence check was initialized or not
     * <p>
     *     Any of the statuses [{@link IdentityVerificationStatus#FAILED},
     *     {@link IdentityVerificationStatus#IN_PROGRESS}, {@link IdentityVerificationStatus#REJECTED}]
     *     means an already initialized presence check.
     * </p>
     * @return true when the presence check is already initialized
     */
    @Transient
    public boolean isPresenceCheckInitialized() {
        return IdentityVerificationPhase.PRESENCE_CHECK.equals(phase) &&
                List.of(IdentityVerificationStatus.FAILED,
                        IdentityVerificationStatus.IN_PROGRESS,
                        IdentityVerificationStatus.REJECTED).contains(status);
    }

}