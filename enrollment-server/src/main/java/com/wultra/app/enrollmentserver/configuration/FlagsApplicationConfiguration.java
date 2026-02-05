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

package com.wultra.app.enrollmentserver.configuration;

import com.wultra.security.powerauth.rest.api.spring.application.PowerAuthApplicationConfiguration;
import com.wultra.security.powerauth.rest.api.spring.model.ActivationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration which adds activation flags into a custom object when getting activation status.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Configuration
@ConditionalOnProperty(name = "enrollment-server.flag.reporting", havingValue = "true")
public class FlagsApplicationConfiguration implements PowerAuthApplicationConfiguration {

    @Override
    public Map<String, Object> statusServiceCustomObject(final ActivationContext activationContext) {
        List<String> activationFlags = activationContext.getActivationFlags();
        Map<String, Object> customObject = new LinkedHashMap<>();
        customObject.put("activationFlags", activationFlags);
        return customObject;
    }
}
