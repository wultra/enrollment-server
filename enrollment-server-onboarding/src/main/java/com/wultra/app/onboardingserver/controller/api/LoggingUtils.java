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
package com.wultra.app.onboardingserver.controller.api;

import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthActivation;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Temporary utility class to extract data for logging.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
final class LoggingUtils {

    private LoggingUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Extract request object from request.
     *
     * @param request request
     * @return request object as optional
     * @param <T> request type
     */
    // TODO (racansky, 2026-02-25, #1589) remove when validation of encryptionContext made implicit
    public static <T> Optional<T> extractRequest(final ObjectRequest<T> request) {
        return Optional.ofNullable(request).map(ObjectRequest::getRequestObject);
    }

    /**
     * Extract activation ID from authentication.
     *
     * @param apiAuthentication authentication
     * @return activation ID or {@code null} if not available
     */
    public static @Nullable String extractActivationId(final PowerAuthApiAuthentication apiAuthentication) {
        return Optional.ofNullable(apiAuthentication)
                .map(PowerAuthApiAuthentication::getActivationContext)
                .map(PowerAuthActivation::getActivationId)
                .orElse(null);
    }
}
