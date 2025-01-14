/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2024 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.api.model.enrollment.response;

import lombok.Data;

/**
 * Response object for OIDC application configuration.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
public class OidcApplicationConfigurationResponse {

    private String providerId;
    private String clientId;
    private String scopes;
    private String authorizeUri;
    private String redirectUri;

    /**
     * A hint for the mobile application whether to user PKCE.
     * If set to {@code true}, {@code codeVerifier} must be present in identity attributes during create activation step.
     */
    private boolean pkceEnabled;

}
