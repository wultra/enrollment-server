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
package com.wultra.app.onboardingserver.presencecheck.iproov.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wultra.app.onboardingserver.presencecheck.iproov.model.api.AuthTokenResponse;
import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.AbstractWebClientReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * iProov configuration.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.presence-check.provider", havingValue = "iproov")
@ComponentScan(basePackages = {"com.wultra.app.onboardingserver.presencecheck"})
@Configuration
@Slf4j
public class IProovConfig {

    private static final String OAUTH_REGISTRATION_ID = "iproov";

    /**
     * @return Object mapper bean specific to iProov json format
     */
    @Bean("objectMapperIproov")
    public ObjectMapper objectMapperIproov() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    /**
     * Prepares REST client specific to iProov
     * @param configProps Configuration properties
     * @return REST client for iProov service API calls
     */
    @Bean("restClientIProov")
    public RestClient restClientIProov(IProovConfigProps configProps) throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, configProps.getServiceUserAgent());

        RestClientConfiguration restClientConfiguration = configProps.getRestClientConfig();
        restClientConfiguration.setBaseUrl(configProps.getServiceBaseUrl());
        restClientConfiguration.setDefaultHttpHeaders(headers);
        return new DefaultRestClient(restClientConfiguration);
    }


    @Bean
    public WebClient iproovManagemenentWebClient(final IProovConfigProps configProps) {
        final AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = authorizedClientServiceReactiveOAuth2AuthorizedClientManager(configProps);

        final ServerOAuth2AuthorizedClientExchangeFilterFunction oAuth2ExchangeFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oAuth2ExchangeFilterFunction.setDefaultClientRegistrationId(OAUTH_REGISTRATION_ID);

        return createWebClient(oAuth2ExchangeFilterFunction, configProps, "user management client");
    }

    private static AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientServiceReactiveOAuth2AuthorizedClientManager(final IProovConfigProps configProps) {
        final String tokenUri = UriComponentsBuilder.fromHttpUrl(configProps.getServiceBaseUrl() + "/{apiKey}/access_token")
                .buildAndExpand(configProps.getApiKey())
                .toUriString();
        logger.debug("Resolved tokenUri: {}", tokenUri);
        final ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(OAUTH_REGISTRATION_ID)
                .tokenUri(tokenUri)
                .clientName(configProps.getServiceUserAgent())
                .clientId(configProps.getOAuthClientUsername())
                .clientSecret(configProps.getOAuthClientPassword())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        final ReactiveClientRegistrationRepository clientRegistrations = new InMemoryReactiveClientRegistrationRepository(clientRegistration);
        final ReactiveOAuth2AuthorizedClientService clientService = new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrations);

        final ClientCredentialsReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();
        authorizedClientProvider.setAccessTokenResponseClient(accessTokenResponseClient(configProps));

        final AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrations, clientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    // TODO (racansky, 2023-06-05) remove when iProov fix API according the RFC
    private static ReactiveOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> accessTokenResponseClient(final IProovConfigProps configProps) {
        @SuppressWarnings("unchecked")
        final ExchangeFilterFunction tokenResponseFilter = ExchangeFilterFunction.ofResponseProcessor(response -> {
        final ClientResponse.Builder builder = response.mutate();
        return response.bodyToMono(Map.class).map(map -> {
                    logger.trace("Got access token");
                    if (map.containsKey(AuthTokenResponse.JSON_PROPERTY_SCOPE)) {
                        logger.debug("Removing scope because does not comply with RFC and not needed anyway");
                        map.remove(AuthTokenResponse.JSON_PROPERTY_SCOPE);
                        return builder.body(JSONObject.toJSONString(map)).build();
                    } else {
                        return builder.build();
                    }
                })
                .doOnError(e -> {
                    if (e instanceof final WebClientResponseException exception) {
                        logger.error("Get access token - Error response body: {}", exception.getResponseBodyAsString());
                    } else {
                        logger.error("Get access token - Error", e);
                    }
                });
        });

        final WebClient webClient = createWebClient(tokenResponseFilter, configProps, "oAuth client");

        final AbstractWebClientReactiveOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> accessTokenResponseClient = new WebClientReactiveClientCredentialsTokenResponseClient();
        accessTokenResponseClient.setWebClient(webClient);
        return accessTokenResponseClient;
    }

    private static WebClient createWebClient(final ExchangeFilterFunction filter, IProovConfigProps configProps, final String logContext) {
        final RestClientConfiguration restClientConfig = configProps.getRestClientConfig();
        final Integer connectionTimeout = restClientConfig.getConnectionTimeout();
        final Duration responseTimeout = restClientConfig.getResponseTimeout();
        final Duration maxIdleTime = Objects.requireNonNull(restClientConfig.getMaxIdleTime(), "maxIdleTime must be specified");
        logger.info("Setting {} connectionTimeout: {}, responseTimeout: {}, maxIdleTime: {}", logContext, connectionTimeout, responseTimeout, maxIdleTime);

        final ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxIdleTime(maxIdleTime)
                .build();
        final HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .responseTimeout(responseTimeout);
        final ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        return WebClient.builder()
                .clientConnector(connector)
                .filter(filter)
                .build();
    }

}
