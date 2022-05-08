# Configuration Properties

The Enrollment Server uses the following public configuration properties:

## Database Configuration

| Property | Default | Note |
|---|---|---|
| `spring.datasource.url` | `_empty_` | Database JDBC URL |
| `spring.datasource.username` | `_empty_` | Database JDBC username |
| `spring.datasource.password` | `_empty_` | Database JDBC password |
| `spring.datasource.driver-class-name` | `_empty_` | Datasource JDBC class name | 
| `spring.jpa.database-platform` | `_empty_` | Database dialect | 
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
| `enrollment-server.activation-spawn.enabled` | `false` | The activation spawn functionality can be enabled or disabled using this property. |

## Correlation HTTP Header Configuration

| Property | Default | Note |
|---|---|---|
| `powerauth.service.correlation-header.enabled` | `false` | Whether correlation header is enabled |
| `powerauth.service.correlation-header.name` | `X-Correlation-ID` | Correlation header name |
| `powerauth.service.correlation-header.value.validation-regexp` | `[a-zA-Z0-9\\-]{8,1024}` | Regular expression for correlation header value validation |
| `logging.pattern.console` | [See application.properties](https://github.com/wultra/enrollment-server/blob/develop/enrollment-server/src/main/resources/application.properties#L160) | Logging pattern for console which includes the correlation header value |