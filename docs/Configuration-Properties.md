# Configuration Properties

The Enrollment Server uses the following public configuration properties:

## Database Configuration

| Property | Default | Note |
|---|---|---|
| `spring.datasource.url` | `_empty_` | Database JDBC URL |
| `spring.datasource.username` | `_empty_` | Database JDBC username |
| `spring.datasource.password` | `_empty_` | Database JDBC password |
| `spring.datasource.driver-class-name` | `_empty_` | Datasource JDBC class name | 
| `spring.jpa.hibernate.ddl-auto` | `none` | Configuration of automatic database schema creation | 
| `spring.jpa.properties.hibernate.connection.characterEncoding` | `_empty_` | Character encoding |
| `spring.jpa.properties.hibernate.connection.useUnicode` | `_empty_` | Character encoding - Unicode support |

## PowerAuth Service Configuration

| Property | Default | Note |
|---|---|---|
| `powerauth.service.url` | `http://localhost:8080/powerauth-java-server/rest` | PowerAuth service REST API base URL. | 
| `powerauth.service.security.clientToken` | `_empty_` | PowerAuth REST API authentication token. | 
| `powerauth.service.security.clientSecret` | `_empty_` | PowerAuth REST API authentication secret / password. |

## PowerAuth Push Service Configuration

| Property | Default | Note |
|---|---|---|
| `powerauth.push.service.url` | `http://localhost:8080/powerauth-push-server` | PowerAuth Push service REST API base URL. |

## Enrollment Server Configuration

| Property | Default | Note |
|---|---|---|
| `enrollment-server.mtoken.enabled` | `true` | Publishing of Mobile Token endpoints can be enabled or disabled using this property. |
| `enrollment-server.inbox.enabled` | `true` | Publishing of Inbox endpoints can be enabled or disabled using this property. |
| `enrollment-server.activation-spawn.enabled` | `false` | The activation spawn functionality can be enabled or disabled using this property. |

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

The WAR file includes the `micrometer-registry-prometheus` dependency.
Discuss its configuration with the [Spring Boot documentation](https://docs.spring.io/spring-boot/docs/3.1.x/reference/html/actuator.html#actuator.metrics).
