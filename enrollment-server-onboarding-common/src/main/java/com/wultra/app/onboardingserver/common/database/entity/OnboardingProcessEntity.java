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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Entity representing an onboarding process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "es_onboarding_process")
@Slf4j
public class OnboardingProcessEntity implements Serializable {

    private static final long serialVersionUID = -438495244269415158L;

    public static final String ERROR_PROCESS_CANCELED = "canceledProcess";
    public static final String ERROR_PROCESS_EXPIRED_ACTIVATION = "expiredProcessActivation";
    public static final String ERROR_PROCESS_EXPIRED_IDENTITY_VERIFICATION = "expiredProcessIdentityVerification";
    public static final String ERROR_PROCESS_EXPIRED_ONBOARDING = "expiredProcessOnboarding";
    public static final String ERROR_MAX_FAILED_ATTEMPTS_IDENTITY_VERIFICATION = "maxFailedAttemptsIdentityVerification";
    public static final String ERROR_TOO_MANY_PROCESSES_PER_USER = "tooManyProcessesPerUser";
    public static final String ERROR_MAX_PROCESS_ERROR_SCORE_EXCEEDED = "maxProcessErrorScoreExceeded";
    public static final String ERROR_USER_LOOKUP = "userLookupFailed";

    /**
     * Key for {@link #customData} storing locale.
     */
    private static final String CUSTOM_DATA_LOCALE = "locale";

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "identification_data", nullable = false)
    private String identificationData;

    /**
     * Json with custom data such as preferred locale.
     */
    @Column(name = "custom_data", nullable = false)
    private String customData = "{}";

    @Column(name = "user_id")
    private String userId;

    @Column(name = "activation_id")
    private String activationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OnboardingStatus status;

    /**
     * When the status is {@link OnboardingStatus#FAILED}, the activation specified be {@link #activationId} should be removed at PowerAuth server.
     * This flag indicates that the task has been done.
     */
    @Column(name = "activation_removed", columnDefinition="boolean default false")
    private boolean activationRemoved;

    @Column(name = "error_detail")
    private String errorDetail;

    @Column(name = "error_origin")
    @Enumerated(EnumType.STRING)
    private ErrorOrigin errorOrigin;

    @Column(name = "error_score")
    private int errorScore;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Column(name = "timestamp_last_updated")
    private Date timestampLastUpdated;

    @Column(name = "timestamp_finished")
    private Date timestampFinished;

    @Column(name = "timestamp_failed")
    private Date timestampFailed;

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL)
    @OrderBy("timestampCreated")
    @ToString.Exclude
    private Set<OnboardingOtpEntity> otps = new LinkedHashSet<>();

    /**
     * Set the given locale to {@code customData}.
     *
     * @param locale locale
     * @throws OnboardingProcessException
     */
    @SuppressWarnings("unchecked") // unchecked readValue
    public void setLocale(final Locale locale) throws OnboardingProcessException {
        try {
            logger.debug("Setting locale to custom_data: {} of process ID: {}", customData, id);
            final ObjectMapper mapper = new ObjectMapper();
            final Map<String, Object> json = mapper.readValue(customData, Map.class);
            json.put(CUSTOM_DATA_LOCALE, locale.getLanguage());
            customData = mapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new OnboardingProcessException("Problem to parse custom_data of process ID: " + id, e);
        }
    }

    /**
     * Get locale from {@code customData}.
     *
     * @return locale
     * @throws OnboardingProcessException
     */
    @SuppressWarnings("unchecked") // unchecked readValue
    public Locale getLocale() throws OnboardingProcessException {
        try {
            logger.debug("Getting locale from custom_data: {} of process ID: {}", customData, id);
            final ObjectMapper mapper = new ObjectMapper();
            final Map<String, Object> json = mapper.readValue(customData, Map.class);
            final String language = json.get(CUSTOM_DATA_LOCALE).toString();
            return new Locale(language);
        } catch (JsonProcessingException e) {
            throw new OnboardingProcessException("Problem to parse custom_data of process ID: " + id, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OnboardingProcessEntity)) return false;
        OnboardingProcessEntity that = (OnboardingProcessEntity) o;
        return identificationData.equals(that.identificationData) && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identificationData, timestampCreated);
    }
}

