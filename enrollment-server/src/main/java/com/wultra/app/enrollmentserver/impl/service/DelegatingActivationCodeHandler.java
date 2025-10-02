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
package com.wultra.app.enrollmentserver.impl.service;

import com.wultra.app.enrollmentserver.errorhandling.ActivationCodeException;
import com.wultra.core.annotations.PublicSpi;
import lombok.Builder;

import java.util.List;

/**
 * Delegating callback handler for lifecycle events of activation code.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@PublicSpi
public interface DelegatingActivationCodeHandler {

    /**
     *  Fetch target application ID value based on source application ID. Check if the source application can
     *  activate the target one - if the source application cannot activate the target application,
     *  this method should return {@code null}.
     *
     * @param request request object
     * @return response object or {@code null}
     * @throws ActivationCodeException Thrown in case target application ID could not be retrieved.
     */
    TargetApplicationResponse fetchTargetApplication(TargetApplicationRequest request) throws ActivationCodeException;

    /**
     * Callback method to add new activation flags to activation.
     *
     * @param sourceActivationId Source activation ID (activation used to fetch the code).
     * @param sourceActivationFlags Source activation flags (flags of the activation that initiated the transfer).
     * @param userId User ID (user ID who requested the activation).
     * @param applicationId Application identifier which was used for app lookup (String identifier sent from client).
     * @param sourceAppId Source app ID (the app that initiated the process).
     * @param sourceApplicationRoles Source application roles (roles of the app that initiated the transfer).
     * @param destinationAppId Destination app ID (the app that is to be activated).
     * @param destinationActivationId Destination activation ID (the activation ID of the new activation).
     * @param activationCode Activation code of the new activation.
     * @param activationCodeSignature Activation code signature of the new activation code.
     * @return List of new activation flags for the destination activation.
     * @throws ActivationCodeException Thrown in case activation flag processing fails.
     */
    default List<String> addActivationFlags(String sourceActivationId, List<String> sourceActivationFlags, String userId, String applicationId, String sourceAppId, List<String> sourceApplicationRoles, String destinationAppId, String destinationActivationId, String activationCode, String activationCodeSignature) throws ActivationCodeException {
        return List.of();
    }

    /**
     * Callback method with newly created activation code information.
     *
     * @param sourceActivationId Source activation ID (activation used to fetch the code).
     * @param userId User ID (user ID who requested the activation).
     * @param applicationId Application identifier which was used for app lookup (String identifier sent from client).
     * @param sourceAppId Source app ID (the app that initiated the process).
     * @param destinationAppId Destination app ID (the app that is to be activated).
     * @param destinationActivationId Destination activation ID (the activation ID of the new activation).
     * @param activationCode Activation code of the new activation.
     * @param activationCodeSignature Activation code signature of the new activation code.
     * @throws ActivationCodeException Thrown in case activation code processing fails.
     */
    default void didReturnActivationCode(String sourceActivationId, String userId, String applicationId, String sourceAppId, String destinationAppId, String destinationActivationId, String activationCode, String activationCodeSignature) throws ActivationCodeException {
        // Default implementation does nothing
    }

    @Builder
    record TargetApplicationRequest(String targetApplicationId, String sourceApplicationId) {
    }

    @Builder
    record TargetApplicationResponse(String applicationId, ActivationTransferType type) {
    }

    enum ActivationTransferType {

        /**
         * Keeps the original activation.
         */
        SPAWN,

        /**
         * The original activation should be removed after the new activation is active and confirmed.
         */
        MOVE
    }

}
