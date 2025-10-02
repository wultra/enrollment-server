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
