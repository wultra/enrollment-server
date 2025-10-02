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
package com.wultra.app.enrollmentserver.model.integration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.codec.binary.Base32;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Identification of an owner of an identity-related document.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Data
@ToString(of = {"activationId", "userId"})
public class OwnerId {

    /**
     * Maximum allowed length of the user identification
     */
    public static final int USER_ID_MAX_LENGTH = 255;

    /**
     * Activation identifier.
     */
    private String activationId;

    /**
     * User ID (user ID who requested the activation).
     */
    private String userId;

    /**
     * Secured userId value which can be used safely at external providers
     * <p>
     *     An userId can typically contain a sensitive data (e.g. e-mail address, phone number)
     * </p>
     */
    @Setter(AccessLevel.NONE)
    private String userIdSecured;

    /**
     * Timestamp of the identification context
     */
    private Date timestamp = new Date();

    /**
     * @return Securely hashed user identification.
     * This can be used to hide the original possibly sensitive identity value.
     */
    public String getUserIdSecured() {
        if (userId == null) {
            throw new IllegalStateException("Missing userId value");
        }
        if (userIdSecured == null) {
            try {
                final MessageDigest digest = MessageDigest.getInstance("SHA-256");
                final byte[] usedIdHash = digest.digest(userId.getBytes(StandardCharsets.UTF_8));
                userIdSecured = new String(new Base32().encode(usedIdHash), StandardCharsets.UTF_8)
                        .replace("=", "");
                if (userIdSecured.length() > USER_ID_MAX_LENGTH) {
                    userIdSecured = userIdSecured.substring(0, USER_ID_MAX_LENGTH);
                }
            } catch (NoSuchAlgorithmException e) {
                // Impossible error
            }
        }
        return userIdSecured;
    }

}
