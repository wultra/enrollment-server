/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.common.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Context extension to enable using provider configuration as a SpEL in @Query.
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@Component
public class ProvidersEvaluationContextExtension implements EvaluationContextExtension {

    @Value("${enrollment-server-onboarding.document-verification.provider:mock}")
    private String documentVerificationProvider;

    @Override
    public String getExtensionId() {
        return "providers";
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("documentVerificationProviderName", documentVerificationProvider);
    }

}
