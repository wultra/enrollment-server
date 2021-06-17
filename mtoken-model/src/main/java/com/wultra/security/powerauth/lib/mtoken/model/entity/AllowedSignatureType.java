/*
 * Copyright 2017 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wultra.security.powerauth.lib.mtoken.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        @JsonProperty("1FA")
        MULTIFACTOR_1FA("1FA"),     // 1FA signature
        @JsonProperty("2FA")
        MULTIFACTOR_2FA("2FA"),     // 2FA signature
        @JsonProperty("ECDSA")
        ASYMMETRIC_ECDSA("ECDSA");  // ECDSA private key signature

        private final String type;

        Type(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.type;
        }
    }

    private Type type;
    private List<String> variants;

}
