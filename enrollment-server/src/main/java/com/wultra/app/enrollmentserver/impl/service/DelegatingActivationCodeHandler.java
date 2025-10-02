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
     * Fetch transfer configuration based on source and target application ID.
     * Check if the source application can activate the target one - if the source application cannot activate the target application,
     * this method should return {@code null}.
     *
     * @param request Contains source and target application IDs
     * @return Response containing target application ID, transfer type and initial flags or {@code null}
     * @throws ActivationCodeException Thrown in case the transfer configuration could not be retrieved.
     */
    TransferConfigurationResponse fetchTransferConfiguration(TransferConfigurationRequest request) throws ActivationCodeException;

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
    record TransferConfigurationRequest(String targetApplicationId, String sourceApplicationId) {
    }

    @Builder
    record TransferConfigurationResponse(String applicationId, ActivationTransferType type, List<String> initialFlags) {
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
