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

import java.util.List;

/**
 * Delegating callback handler for lifecycle events of activation code.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
public interface DelegatingActivationCodeHandler {

    /**
     * Fetch destination application ID value based on application ID. Check if the source app can
     * activate the destination one - if the source application cannot activate destination app, this
     * method should return null.
     *
     * @param applicationId Application identifier for app lookup.
     * @param sourceAppId Source application ID.
     * @param activationFlags Activation flags.
     * @param applicationRoles Application roles.
     * @return Destination application ID.
     * @throws ActivationCodeException Thrown in case destination application ID could not be retrieved.
     */
    Long fetchDestinationApplicationId(String applicationId, Long sourceAppId, List<String> activationFlags, List<String> applicationRoles) throws ActivationCodeException;

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
    List<String> addActivationFlags(String sourceActivationId, List<String> sourceActivationFlags, String userId, String applicationId, Long sourceAppId, List<String> sourceApplicationRoles, Long destinationAppId, String destinationActivationId, String activationCode, String activationCodeSignature) throws ActivationCodeException;

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
    void didReturnActivationCode(String sourceActivationId, String userId, String applicationId, Long sourceAppId, Long destinationAppId, String destinationActivationId, String activationCode, String activationCodeSignature) throws ActivationCodeException;

}
