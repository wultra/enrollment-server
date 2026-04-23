# Configuration Properties

The Onboarding Server uses the following public configuration properties:

## Database Configuration

| Property                                                       | Default                                      | Note                                                |
|----------------------------------------------------------------|----------------------------------------------|-----------------------------------------------------|
| `spring.datasource.url`                                        | `jdbc:postgresql://localhost:5432/powerauth` | Database JDBC URL                                   |
| `spring.datasource.username`                                   | `powerauth`                                  | Database JDBC username                              |
| `spring.datasource.password`                                   | `_empty_`                                    | Database JDBC password                              |
| `spring.jpa.hibernate.ddl-auto`                                | `none`                                       | Configuration of automatic database schema creation | 
| `spring.jpa.properties.hibernate.connection.characterEncoding` | `utf8`                                       | Character encoding                                  |
| `spring.jpa.properties.hibernate.connection.useUnicode`        | `true`                                       | Character encoding - Unicode support                |

## PowerAuth Service Configuration

| Property                                             | Default                                            | Note                                                 |
|------------------------------------------------------|----------------------------------------------------|------------------------------------------------------|
| `powerauth.service.url`                              | `http://localhost:8080/powerauth-java-server/rest` | PowerAuth service REST API base URL.                 |
| `powerauth.service.restClientConfig.responseTimeout` | `60s`                                              | PowerAuth REST API response timeout.                 |
| `powerauth.service.restClientConfig.maxIdleTime`     | `200s`                                             | PowerAuth REST API max idle time.                    |
| `powerauth.service.security.clientToken`             | `_empty_`                                          | PowerAuth REST API authentication token.             | 
| `powerauth.service.security.clientSecret`            | `_empty_`                                          | PowerAuth REST API authentication secret / password. |

## Onboarding Process Configuration

The configuration properties below are global to all the onboarding processes.
Specific process options are defined in the database.
See [Configuration of Onboarding Process](./Configuration-Onboarding-Process.md) for the details of JSON configuration.

| Property                                                                  | Default   | Note                                                                                          |
|---------------------------------------------------------------------------|-----------|-----------------------------------------------------------------------------------------------|
| `enrollment-server-onboarding.onboarding-process.otp.length`              | `8`       | Length of generated digital OTP codes.                                                        |
| `enrollment-server-onboarding.onboarding-process.otp.expiration`          | `5m`      | Expiration time for OTP codes.                                                                |
| `enrollment-server-onboarding.onboarding-process.otp.max-failed-attempts` | `5`       | Maximum number of failed attempts for OTP verification.                                       |
| `enrollment-server-onboarding.onboarding-process.otp.resend-period`       | `30s`     | A time period after which next OTP can be sent.                                               |
| `enrollment-server-onboarding.onboarding-process.expiration`              | `3h`      | Onboarding process expiration time.                                                           |
| `enrollment-server-onboarding.onboarding-process.activation.expiration`   | `5m`      | Expiration of activations used within an onboarding process.                                  |
| `enrollment-server-onboarding.onboarding-process.verification.expiration` | `1h`      | Expiration of identity verification within an onboarding process.                             |
| `enrollment-server-onboarding.onboarding-process.max-processes-per-day`   | `5`       | Maximum number of onboarding processes during last 24 hours per user.                         |
| `enrollment-server-onboarding.onboarding-process.max-error-score`         | `15`      | Maximum error score for an onboarding process.                                                |
| `enrollment-server-onboarding.onboarding-process.default-type`            | `_empty_` | Default process type used for starting onboarding if not specified by the mobile application. |

## Identity Verification Configuration

| Property                                                                                 | Default  | Note                                                                                                                                                                                                                                                            |
|------------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enrollment-server-onboarding.identity-verification.enabled`                             | `false`  | Whether identity verification is enabled.                                                                                                                                                                                                                       |
| `enrollment-server-onboarding.identity-verification.max-failed-attempts`                 | `5`      | Maximum failed attempts for identity verification.                                                                                                                                                                                                              |
| `enrollment-server-onboarding.identity-verification.max-failed-attempts-document-upload` | `5`      | Maximum failed attempts for document upload.                                                                                                                                                                                                                    |
| `enrollment-server-onboarding.identity-verification.data-retention`                      |          | Retention duration for identity verification personal data. The effective retention window is computed as: `timestamp_created` + `enrollment-server-onboarding.onboarding-process.expiration` + this value. An empty value means automatic cleanup is disabled. |


## Digital Onboarding Adapter Configuration

| Property                                                                         | Default                 | Note                                                         |
|----------------------------------------------------------------------------------|-------------------------|--------------------------------------------------------------|
| `enrollment-server-onboarding.onboarding-adapter.url`                            | `http://localhost:8090` | Digital onboarding adapter service base URL.                 |
| `enrollment-server-onboarding.onboarding-adapter.connection-timeout`             | `2s`                    | TCP connection timeout.                                      |
| `enrollment-server-onboarding.onboarding-adapter.handshake-timeout`              | `5s`                    | Handshake timeout.                                           |
| `enrollment-server-onboarding.onboarding-adapter.response-timeout`               | `60s`                   | HTTP response timeout.                                       |
| `enrollment-server-onboarding.onboarding-adapter.max-idle-time`                  | `200s`                  | Max idle time in connection pool.                            |
| `enrollment-server-onboarding.onboarding-adapter.max-life-time`                  | `1h`                    | Max life time in connection pool.                            |
| `enrollment-server-onboarding.onboarding-adapter.accept-invalid-ssl-certificate` | `false`                 | Whether invalid SSL certificates are accepted by the client. |
| `enrollment-server-onboarding.onboarding-adapter.http-basic-auth-enabled`        | `false`                 | Whether HTTP Basic authentication is enabled.                |
| `enrollment-server-onboarding.onboarding-adapter.http-basic-auth-username`       |                         | HTTP Basic authentication username.                          |
| `enrollment-server-onboarding.onboarding-adapter.http-basic-auth-password`       |                         | HTTP Basic authentication password.                          |
| `enrollment-server-onboarding.onboarding-adapter.correlation-header.name`        | `X-Correlation-Id`      | HTTP correlation header name.                                |
| `enrollment-server-onboarding.onboarding-adapter.request-id-header.name`         | `X-Request-Id`          | HTTP request ID header name.                                 |
| `enrollment-server-onboarding.onboarding-adapter.headers`                        |                         | Custom HTTP headers configuration.                           |

## Client Evaluation Configuration

| Property                                                                | Default | Note                                                                                                                                     |
|-------------------------------------------------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------|
| `enrollment-server-onboarding.client-evaluation.max-failed-attempts`    | 5       | Number of maximum failed attempts for client evaluation.                                                                                 |
| `enrollment-server-onboarding.client-evaluation.include-extracted-data` | `false` | Include extracted data to the evaluate client request. The format of extracted data is defined by the provider of document verification. |

## Document Verification Provider Configuration

| Property                                                                                   | Default         | Note                                                                                  |
|--------------------------------------------------------------------------------------------|-----------------|---------------------------------------------------------------------------------------|
| `enrollment-server-onboarding.document-verification.provider`                              | `mock`          | Document verification provider (`mock`, `zenid`, `innovatrics`).                      |
| `enrollment-server-onboarding.document-verification.cleanupEnabled`                        | `false`         | Whether document cleanup is enabled for the provider.                                 |

## Presence Check Provider Configuration

| Property                                                                       | Default | Note                                                                                   |
|--------------------------------------------------------------------------------|---------|----------------------------------------------------------------------------------------|
| `enrollment-server-onboarding.presence-check.enabled`                          | `true`  | Whether presence check provider is enabled.                                            |
| `enrollment-server-onboarding.presence-check.provider`                         | `mock`  | Presence check provider (`mock`, `iproov`, `innovatrics`).                             |
| `enrollment-server-onboarding.presence-check.cleanupEnabled`                   | `false` | Whether cleanup of presence check data is enabled.                                     |
| `enrollment-server-onboarding.presence-check.verifySelfieWithDocumentsEnabled` | `false` | Whether verification of the presence check selfie photo with the documents is enabled. |
| `enrollment-server-onboarding.presence-check.max-failed-attempts`              | `5`     | Maximum failed attempts for presence check and OTP verification.                       |
| `enrollment-server-onboarding.presence-check.max-failed-attempts`              | `5`     | Maximum failed attempts for presence check and OTP verification.                       |
| `enrollment-server-onboarding.presence-check.selfie.minimal-width`             | `400`   | Minimal width of selfie image (pixels) used to crosscheck presence.                    | 

## Zen ID Configuration

| Property                                                                                                | Default                   | Note                                                                           |
|---------------------------------------------------------------------------------------------------------|---------------------------|--------------------------------------------------------------------------------|
| `enrollment-server-onboarding.document-verification.zenid.apiKey`                                       |                           | Zen ID REST API key.                                                           |
| `enrollment-server-onboarding.document-verification.zenid.additionalDocSubmitValidationsEnabled`        | `true`                    | Whether additional document validations are applied after document submission. |
| `enrollment-server-onboarding.document-verification.zenid.asyncProcessingEnabled`                       | `false`                   | Whether asynchronous processing is enabled for Zen ID provider.                |
| `enrollment-server-onboarding.document-verification.zenid.documentCountry`                              | `Cz`                      | Zen ID country configuration for submitted documents.                          |
| `enrollment-server-onboarding.document-verification.zenid.serviceBaseUrl`                               |                           | Base REST service URL for Zen ID.                                              |
| `enrollment-server-onboarding.document-verification.zenid.serviceUserAgent`                             | `Wultra/OnboardingServer` | User agent to use when making HTTP calls to Zen ID REST service.               |
| `enrollment-server-onboarding.document-verification.zenid.profile`                                      |                           | Optional profile name to determine Zen ID validators configuration.            |
| `enrollment-server-onboarding.document-verification.zenid.restClientConfig.acceptInvalidSslCertificate` | `false`                   | Whether invalid SSL certificate is accepted when calling Zen ID REST service.  |
| `enrollment-server-onboarding.document-verification.zenid.restClientConfig.maxInMemorySize`             | `10485760`                | Maximum in memory size of HTTP requests when calling Zen ID REST service.      |
| `enrollment-server-onboarding.document-verification.zenid.restClientConfig.proxyEnabled`                | `false`                   | Whether proxy server is enabled when calling Zen ID REST service.              |
| `enrollment-server-onboarding.document-verification.zenid.restClientConfig.proxyHost`                   |                           | Proxy host to be used when calling Zen ID REST service.                        |
| `enrollment-server-onboarding.document-verification.zenid.restClientConfig.proxyPort`                   | 0                         | Proxy port to be used when calling Zen ID REST service.                        |
| `enrollment-server-onboarding.document-verification.zenid.restClientConfig.proxyUsername`               |                           | Proxy username to be used when calling Zen ID REST service.                    |
| `enrollment-server-onboarding.document-verification.zenid.restClientConfig.proxyPassword`               |                           | Proxy password to be used when calling Zen ID REST service.                    |

## iProov Configuration

| Property                                                                                          | Default                   | Note                                                                          |
|---------------------------------------------------------------------------------------------------|---------------------------|-------------------------------------------------------------------------------|
| `enrollment-server-onboarding.presence-check.iproov.apiKey`                                       |                           | iProov REST API key.                                                          |
| `enrollment-server-onboarding.presence-check.iproov.apiSecret`                                    |                           | iProov REST API secret.                                                       |
| `enrollment-server-onboarding.presence-check.iproov.assuranceType`                                | `genuine_presence`        | iProov assurance type.                                                        |
| `enrollment-server-onboarding.presence-check.iproov.ensureUserIdValueEnabled`                     | `false`                   | Whether iProov user ID value should be validated and trimmed.                 |
| `enrollment-server-onboarding.presence-check.iproov.riskProfile`                                  |                           | iProov risk profile.                                                          |
| `enrollment-server-onboarding.presence-check.iproov.serviceBaseUrl`                               |                           | Base REST service URL for iProov.                                             |
| `enrollment-server-onboarding.presence-check.iproov.serviceUserAgent`                             | `Wultra/OnboardingServer` | User agent to use when making HTTP calls to iProov REST service.              |
| `enrollment-server-onboarding.presence-check.iproov.oAuthClientUsername`                          |                           | OAuth client username to iProov REST service.                                 |
| `enrollment-server-onboarding.presence-check.iproov.oAuthClientPassword`                          |                           | OAuth client password to iProov REST service.                                 |
| `enrollment-server-onboarding.presence-check.iproov.restClientConfig.acceptInvalidSslCertificate` | `false`                   | Whether invalid SSL certificate is accepted when calling Zen ID REST service. |
| `enrollment-server-onboarding.presence-check.iproov.restClientConfig.maxInMemorySize`             | `10485760`                | Maximum in memory size of HTTP requests when calling iProov REST service.     |
| `enrollment-server-onboarding.presence-check.iproov.restClientConfig.proxyEnabled`                | `false`                   | Whether proxy server is enabled when calling iProov REST service.             |
| `enrollment-server-onboarding.presence-check.iproov.restClientConfig.proxyHost`                   |                           | Proxy host to be used when calling iProov REST service.                       |
| `enrollment-server-onboarding.presence-check.iproov.restClientConfig.proxyPort`                   | 0                         | Proxy port to be used when calling iProov REST service.                       |
| `enrollment-server-onboarding.presence-check.iproov.restClientConfig.proxyUsername`               |                           | Proxy username to be used when calling iProov REST service.                   |
| `enrollment-server-onboarding.presence-check.iproov.restClientConfig.proxyPassword`               |                           | Proxy password to be used when calling iProov REST service.                   |

## Innovatrics Configuration

| Property                                                                                                | Default                   | Note                                                                                                                                                                                                                                                               |
|---------------------------------------------------------------------------------------------------------|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enrollment-server-onboarding.provider.innovatrics.serviceBaseUrl`                                      |                           | Base REST service URL for Innovatrics.                                                                                                                                                                                                                             |
| `enrollment-server-onboarding.provider.innovatrics.serviceToken`                                        |                           | Authentication token for Innovatrics.                                                                                                                                                                                                                              |
| `enrollment-server-onboarding.provider.innovatrics.serviceUserAgent`                                    | `Wultra/OnboardingServer` | User agent to use when making HTTP calls to Innovatrics REST service.                                                                                                                                                                                              |
| `enrollment-server-onboarding.provider.innovatrics.presenceCheckConfiguration.score`                    | 0.875                     | Presence check minimal score threshold.                                                                                                                                                                                                                            |
| `enrollment-server-onboarding.provider.innovatrics.documentVerificationConfiguration.documentCountries` | `CZE`                     | List of expected countries of issue of identification documents as three-letter country codes, i.e. ISO 3166-1 alpha-3. If empty, all countries of issue known to Innovatrics are considered during classification, which may have negative impact on performance. |
| `enrollment-server-onboarding.provider.innovatrics.restClientConfig.acceptInvalidSslCertificate`        | `false`                   | Whether invalid SSL certificate is accepted when calling Zen ID REST service.                                                                                                                                                                                      |
| `enrollment-server-onboarding.provider.innovatrics.restClientConfig.maxInMemorySize`                    | `10485760`                | Maximum in memory size of HTTP requests when calling Innovatrics REST service.                                                                                                                                                                                     |
| `enrollment-server-onboarding.provider.innovatrics.restClientConfig.proxyEnabled`                       | `false`                   | Whether proxy server is enabled when calling Innovatrics REST service.                                                                                                                                                                                             |
| `enrollment-server-onboarding.provider.innovatrics.restClientConfig.proxyHost`                          |                           | Proxy host to be used when calling Innovatrics REST service.                                                                                                                                                                                                       |
| `enrollment-server-onboarding.provider.innovatrics.restClientConfig.proxyPort`                          | 0                         | Proxy port to be used when calling Innovatrics REST service.                                                                                                                                                                                                       |
| `enrollment-server-onboarding.provider.innovatrics.restClientConfig.proxyUsername`                      |                           | Proxy username to be used when calling Innovatrics REST service.                                                                                                                                                                                                   |
| `enrollment-server-onboarding.provider.innovatrics.restClientConfig.proxyPassword`                      |                           | Proxy password to be used when calling Innovatrics REST service.                                                                                                                                                                                                   |

See [Innovatrics documentation](https://developers.innovatrics.com/digital-onboarding/docs/functionalities/face/active-liveness-check/#magnifeye-liveness)
for details how the score affects false acceptances (FAR) and false rejections (FRR).

## Microblink Configuration

| Property                                                                                                | Default | Note                                                                                                                                                                                                                          |
|---------------------------------------------------------------------------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enrollment-server-onboarding.document-verification.microblink.restClientConfig.baseUrl`                |         | Base REST service URL for Microblink.                                                                                                                                                                                         |
| `enrollment-server-onboarding.document-verification.microblink.restClientConfig.httpBasicAuthEnabled`   | `true`  | Whether HTTP Basic authentication is enabled.                                                                                                                                                                                 |
| `enrollment-server-onboarding.document-verification.microblink.restClientConfig.httpBasicAuthUsername`  |         | HTTP Basic authentication username.                                                                                                                                                                                           |
| `enrollment-server-onboarding.document-verification.microblink.restClientConfig.httpBasicAuthPassword`  |         | HTTP Basic authentication password.                                                                                                                                                                                           |
| `enrollment-server-onboarding.document-verification.microblink.restClientConfig.connectionTimeout`      | 5s      | REST connection timeout.                                                                                                                                                                                                      |
| `enrollment-server-onboarding.document-verification.microblink.restClientConfig.responseTimeout`        | 60s     | REST response timeout.                                                                                                                                                                                                        |
| `enrollment-server-onboarding.document-verification.microblink.restClientConfig.maxIdleTime`            | 200s    | Max idle time in connection pool.                                                                                                                                                                                             |
| `enrollment-server-onboarding.document-verification.microblink.restClientConfig.maxLifeTime`            | 1h      | Max life time in connection pool.                                                                                                                                                                                             |
| `enrollment-server-onboarding.document-verification.microblink.mobile-sdk-configs[i].origin`            |         | Microblink Bundle ID / App ID. Together with `platform` forms a composite key for obtaining `license-key` and must match the `origin` attribute in the `POST /api/identity/document/init-sdk` request.                        |
| `enrollment-server-onboarding.document-verification.microblink.mobile-sdk-configs[i].platform`          |         | Mobile platform. Example values: `ios`, `android`, or `cordova`. Together with `origin`, this must match the `platform` attribute in the `POST /api/identity/document/init-sdk` request for the `license-key` to be returned. |
| `enrollment-server-onboarding.document-verification.microblink.mobile-sdk-configs[i].license-key`       |         | Mobile SDK license key for the given `origin` and `platform` combination; returned only when the configured `origin` / `platform` pair matches the `POST /api/identity/document/init-sdk` request attributes.                 |
| `enrollment-server-onboarding.document-verification.microblink.extractedDataCheckEnabled`               | `true`  | Whether check/validation of extracted data is enabled.                                                                                                                                                                        |
| `enrollment-server-onboarding.document-verification.microblink.request-options.returnFaceImage`         | `true`  | Whether the face image extracted from the document should be returned in the response.                                                                                                                                        |
| `enrollment-server-onboarding.document-verification.microblink.request-options.returnImageFormat`       | `Jpg`   | Format of images extracted from document in the response. Supported values: `Jpg`, `Png`, `Qoi`                                                                                                                               |
| `enrollment-server-onboarding.document-verification.microblink.request-options.returnFullDocumentImage` | `true`  | Whether the full document images (front and back sides) should be returned in the response.                                                                                                                                   |

## Correlation HTTP Header Configuration

| Property                                                       | Default                  | Note                                                                    |
|----------------------------------------------------------------|--------------------------|-------------------------------------------------------------------------|
| `powerauth.service.correlation-header.enabled`                 | `false`                  | Whether correlation header is enabled                                   |
| `powerauth.service.correlation-header.name`                    | `X-Correlation-ID`       | Correlation header name                                                 |
| `powerauth.service.correlation-header.value.validation-regexp` | `[a-zA-Z0-9\\-]{8,1024}` | Regular expression for correlation header value validation              |
| `logging.pattern.console`                                      | `_disabled_`             | Logging pattern for console which includes the correlation header value |

Sample setting of logging pattern:

```properties
logging.pattern.console=%clr(%d{${LOG_DATEFORMAT_PATTERN:yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:%5p}) [%X{X-Correlation-ID}] %clr(%5p) %clr(${PID: }){magenta} %clr(---){faint}%clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:%wEx}
```


## REST Service Authentication Configuration

Access to private integration REST services is restricted to users with elevated privileges.
Please select your preferred authentication type and configure the settings bellow accordingly.

| Property                                          | Default      | Note                                                         |
|---------------------------------------------------|--------------|--------------------------------------------------------------|
| `enrollment-server-onboarding.security.auth-type` | `basic_auth` | REST API authentication type. Either `basic_auth` or `oidc`. |


### Basic Authentication

If you select basic HTTP authentication, the following configuration properties may need to be adjusted.

| Property                        | Note                                                                                                                                                                                                                  |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `spring.security.user.name`     | User name                                                                                                                                                                                                             |
| `spring.security.user.password` | User password `{id}encodedPassword`, see [Spring Password Storage Format](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html#authentication-password-storage-dpe-format). |


### OpenID Connect

If you select OpenID Connect for authentication, review the following configuration properties to ensure they meet your requirements.

| Property                                                      | Default | Note                                                                                            |
|---------------------------------------------------------------|---------|-------------------------------------------------------------------------------------------------|
| `spring.security.oauth2.resource-server.jwt.issuer-uri`       |         | URL of the authorization server.                                                                |
| `spring.security.oauth2.resource-server.jwt.audiences`        |         | A comma-separated list of allowed `aud` JWT claim values to be validated.                       |


## Monitoring and Observability

| Property                                  | Default | Note                                                                                                                                                                        |
|-------------------------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `management.tracing.sampling.probability` | `1.0`   | Specifies the proportion of requests that are sampled for tracing. A value of 1.0 means that 100% of requests are sampled, while a value of 0 effectively disables tracing. |

The WAR file includes the `micrometer-registry-prometheus` dependency.
Discuss its configuration with
the [Spring Boot documentation](https://docs.spring.io/spring-boot/docs/3.1.x/reference/html/actuator.html#actuator.metrics).
