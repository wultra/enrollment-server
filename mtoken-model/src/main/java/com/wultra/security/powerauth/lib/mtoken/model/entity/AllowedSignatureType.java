/*
 * PowerAuth Mobile Token Model
 * Copyright (C) 2017 Wultra s.r.o.
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
package com.wultra.security.powerauth.lib.mtoken.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Object representing types of signatures that are admissible for
 * a given operation approval.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
public class AllowedSignatureType {

    /**
     * Signature types.
     */
    public enum Type {

        /**
         * Symmetric multi-factor signature with 1FA (possession)
         */
        @JsonProperty("1FA")
        MULTIFACTOR_1FA("1FA"),     // 1FA signature

        /**
         * Symmetric multi-factor signature with 2FA (possession_knowledge, possession_biometry)
         */
        @JsonProperty("2FA")
        MULTIFACTOR_2FA("2FA"),     // 2FA signature

        /**
         * Asymmetric signature private key (ECDSA)
         */
        @JsonProperty("ECDSA")
        ASYMMETRIC_ECDSA("ECDSA");  // ECDSA private key signature

        private final String type;

        /**
         * Constructor with type.
         * @param type Type of the signature.
         */
        Type(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.type;
        }
    }

    @NotNull
    private Type type;

    @NotNull
    private List<String> variants = new ArrayList<>();

}
