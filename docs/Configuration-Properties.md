# Configuration Properties

The Enrollment Server uses the following public configuration properties:

## Database Configuration

| Property | Default | Note |
|---|---|---|
| `spring.datasource.url` | `_empty_` | Database JDBC URL |
| `spring.datasource.username` | `_empty_` | Database JDBC username |
| `spring.datasource.password` | `_empty_` | Database JDBC password |
| `spring.jpa.hibernate.ddl-auto` | `none` | Configuration of automatic database schema creation | 
| `spring.jpa.properties.hibernate.connection.characterEncoding` | `_empty_` | Character encoding |
| `spring.jpa.properties.hibernate.connection.useUnicode` | `_empty_` | Character encoding - Unicode support |

## PowerAuth Service Configuration

| Property                                             | Default                                            | Note                                                 |
|------------------------------------------------------|----------------------------------------------------|------------------------------------------------------|
| `powerauth.service.url`                              | `http://localhost:8080/powerauth-java-server/rest` | PowerAuth service REST API base URL.                 |
| `powerauth.service.restClientConfig.responseTimeout` | `60s`                                              | PowerAuth REST API response timeout.                 |
| `powerauth.service.restClientConfig.maxIdleTime`     | `200s`                                             | PowerAuth REST API max idle time.                    |
| `powerauth.service.security.clientToken`             | `_empty_`                                          | PowerAuth REST API authentication token.             |
| `powerauth.service.security.clientSecret`            | `_empty_`                                          | PowerAuth REST API authentication secret / password. |

## PowerAuth Push Service Configuration

| Property                                                  | Default                                       | Note                                              |
|-----------------------------------------------------------|-----------------------------------------------|---------------------------------------------------|
| `powerauth.push.service.url`                              | `http://localhost:8080/powerauth-push-server` | PowerAuth Push service REST API base URL.         |
| `powerauth.push.service.restClientConfig.responseTimeout` | `60s`                                         | PowerAuth Push service REST API response timeout. |
| `powerauth.push.service.restClientConfig.maxIdleTime`     | `200s`                                        | PowerAuth Push service REST API max idle time.    |

## Enrollment Server Configuration

| Property                                                | Default | Note                                                                                                                                                                                                                                       |
|---------------------------------------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enrollment-server.mtoken.enabled`                      | `true`  | Publishing of Mobile Token endpoints can be enabled or disabled using this property.                                                                                                                                                       |
| `enrollment-server.inbox.enabled`                       | `true`  | Publishing of Inbox endpoints can be enabled or disabled using this property.                                                                                                                                                              |
| `enrollment-server.activation-spawn.enabled`            | `false` | The activation spawn functionality can be enabled or disabled using this property.                                                                                                                                                         |
| `enrollment-server.admin.enabled`                       | `false` | The admin API can be enabled or disabled using this property.                                                                                                                                                                              |
| `enrollment-server.auth-type`                           | `NONE`  | `BASIC_HTTP` for basic HTTP authentication or `OIDC` for OpenID Connect. If authentication enabled, the corresponding properties bellow must be configured.                                                                                |
| `spring.security.user.name`                             |         | Basic HTTP property, user name                                                                                                                                                                                                             |
| `spring.security.user.password`                         |         | Basic HTTP property, user password `{id}encodedPassword`, see [Spring Password Storage Format](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html#authentication-password-storage-dpe-format). | 
| `spring.security.oauth2.resource-server.jwt.issuer-uri` |         | OIDC property, URL of the provider, e.g. `https://sts.windows.net/example/`                                                                                                                                                                |
| `spring.security.oauth2.resource-server.jwt.audiences`  |         | OIDC property, a comma-separated list of allowed `aud` JWT claim values to be validated.                                                                                                                                                   |

## UserInfoProvider Configuration

| Property                                                                           | Default              | Note                                                                                                                                                                                           |
|------------------------------------------------------------------------------------|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enrollment-server.user-info.provider`                                             | `_empty_`            | Whether to register minimal claims provider (value `MINIMAL`) or REST provider (value `REST`).                                                                                                 |
| `enrollment-server.user-info.rest-provider.allowed-stages`                         | `USER_INFO_ENDPOINT` | Stages from where is allowed to request the user info. Possible values: `ACTIVATION_PROCESS_ACTIVATION_CODE`, `ACTIVATION_PROCESS_CUSTOM`, `ACTIVATION_PROCESS_RECOVERY`, `USER_INFO_ENDPOINT` |
| `enrollment-server.user-info.rest-provider.restClientConfig.baseUrl`               | `_empty_`            | Base URL of user-info storage. Must be specified if the provider is type of `REST`.                                                                                                            |
| `enrollment-server.user-info.rest-provider.restClientConfig.httpBasicAuthEnabled`  | `false`              | Whether Basic authentication enabled.                                                                                                                                                          |
| `enrollment-server.user-info.rest-provider.restClientConfig.httpBasicAuthUsername` | `_empty_`            | Basic authentication username.                                                                                                                                                                 |
| `enrollment-server.user-info.rest-provider.restClientConfig.httpBasicAuthPassword` | `_empty_`            | Basic authentication password.                                                                                                                                                                 |

## Correlation HTTP Header Configuration

| Property | Default | Note |
|---|---|---|
| `powerauth.service.correlation-header.enabled` | `false` | Whether correlation header is enabled |
| `powerauth.service.correlation-header.name` | `X-Correlation-ID` | Correlation header name |
| `powerauth.service.correlation-header.value.validation-regexp` | `[a-zA-Z0-9\\-]{8,1024}` | Regular expression for correlation header value validation |
| `logging.pattern.console` | `_disabled_` | Logging pattern for console which includes the correlation header value |

Sample setting of logging pattern:
```properties
logging.pattern.console=%clr(%d{${LOG_DATEFORMAT_PATTERN:yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:%5p}) [%X{X-Correlation-ID}] %clr(%5p) %clr(${PID: }){magenta} %clr(---){faint}%clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:%wEx}
```


## Monitoring and Observability

| Property                                  | Default | Note                                                                                                                                                                        |
|-------------------------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `management.tracing.sampling.probability` | `1.0`   | Specifies the proportion of requests that are sampled for tracing. A value of 1.0 means that 100% of requests are sampled, while a value of 0 effectively disables tracing. |

The WAR file includes the `micrometer-registry-prometheus` dependency.
Discuss its configuration with the [Spring Boot documentation](https://docs.spring.io/spring-boot/docs/3.1.x/reference/html/actuator.html#actuator.metrics).
