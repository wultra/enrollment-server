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

package com.wultra.app.enrollmentserver.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;

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

    @Value("${enrollment-server.auth-type}")
    private AuthType authType;

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        Assert.state(authType != null, "No authentication type configured.");

        if (authType == AuthType.NONE) {
            logger.info("No authentication.");
            http.httpBasic(AbstractHttpConfigurer::disable);
        } else {
            http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(new AntPathRequestMatcher("/api/admin/**")).authenticated()
                    .anyRequest().permitAll());
        }

        if (authType == AuthType.BASIC_HTTP) {
            logger.info("Initializing HTTP basic authentication.");
            http.httpBasic(withDefaults());
        } else if (authType == AuthType.OIDC) {
            logger.info("Initializing OIDC authentication.");
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        }

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @ConditionalOnProperty(value = "enrollment-server.auth-type", havingValue = "BASIC_HTTP" )
    @Bean
    public UserDetailsService userDetailsService(final SecurityProperties securityProperties) {
        final String username = securityProperties.getUser().getName();
        Assert.hasLength(username, "Username must not be blank.");
        logger.info("Initializing user detail service for: {}", username);
        final UserDetails user = User.withUsername(username)
                .password(securityProperties.getUser().getPassword())
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @ConditionalOnProperty(value = "enrollment-server.auth-type", havingValue = "BASIC_HTTP" )
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    enum AuthType {
        NONE,
        BASIC_HTTP,

        /**
         * OpenID Connect.
         */
        OIDC
    }

}
