/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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

package com.wultra.app.onboardingserver.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security configuration.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Value("${enrollment-server-onboarding.security.auth-type}")
    private AuthType authType;

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        if (authType == AuthType.BASIC_AUTH) {
            logger.info("Initializing HTTP basic authentication.");
            http.httpBasic(withDefaults());
        } else if (authType == AuthType.OIDC) {
            logger.info("Initializing OIDC authentication.");
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        }

        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/private/**").authenticated()
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    enum AuthType {

        /**
         * Basic HTTP authentication.
         */
        BASIC_AUTH,

        /**
         * OpenID Connect.
         */
        OIDC
    }
}
