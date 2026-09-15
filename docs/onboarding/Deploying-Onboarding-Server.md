# Deploying Onboarding Server

This chapter explains how to deploy Onboarding Server.

## Downloading Onboarding Server

You can download the latest `enrollment-server-onboarding.war` from the [Enrollment Server releases page](https://github.com/wultra/enrollment-server/releases).

## Configuring Onboarding Server

See chapter [Configuration Properties](./Configuration-Properties.md).

## Deploying Onboarding Server

You can deploy Onboarding Server into any Java EE container.

The default configuration works best with Apache Tomcat server running on default port 8080. In this case, the deployed server is accessible on `http://localhost:8080/enrollment-server-onboarding/`.

To deploy Onboarding Server to Apache Tomcat, simply copy the WAR file in your `webapps` folder or deploy it using the "Tomcat Web Application Manager" application (usually deployed on default Tomcat address `http://localhost:8080/manager`).

Running Onboarding Server from console using the `java -jar` command is not supported.

## Correlation Header Configuration (Optional)

You can enable correlation header logging in Onboarding server by enabling the following properties:

```properties
powerauth.service.correlation-header.enabled=true
powerauth.service.correlation-header.name=X-Correlation-ID
powerauth.service.correlation-header.value.validation-regexp=[a-zA-Z0-9\\-]{8,1024}
logging.pattern.console=%clr(%d{${LOG_DATEFORMAT_PATTERN:yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:%5p}) [%X{X-Correlation-ID}] %clr(%5p) %clr(${PID: }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%sa%n${LOG_EXCEPTION_CONVERSION_WORD:%wEx}
```

This keeps structured key-value information on the same log line.

> **Warning:** If you customize `logging.pattern.console` and omit `%sa`, structured
> log data (e.g. action, state, result, IDs, and other useful parameters attached to
> log messages) will be silently dropped from the console output.

Update the correlation header name in case you want to use a different HTTP header than `X-Correlation-ID`. You can also update the regular expression for correlation header value validation to match the exact format of correlation header value that will be used.

The logging pattern for console is the Spring default logging pattern with the addition of `%X{X-Correlation-ID}`. This variable is used to log the actual value of the correlation header.

For best traceability, the correlation headers should be enabled in the whole PowerAuth stack, so enable the correlation headers in other deployed applications, too. The configuration property names are the same in all PowerAuth applications: `powerauth.service.correlation-header.*`. The correlation header values are passed through the stack, so the requests can be traced easily across multiple components.

## Logging


By default, Onboarding Server logs in a human-readable, plain-text format to the
console. Structured key-value data attached to log messages (e.g. action or state
information) is included as well, appended at the end of each log line.

If you prefer structured JSON logs (for example for log aggregation tools), you can
switch to JSON logging by setting the `ENROLLMENT_SERVER_ONBOARDING_LOGGING`
environment variable to `/opt/logback/conf/eso-logback.xml`.

## Deploying Onboarding Server On JBoss / Wildfly

Follow the extra instructions in chapter [Deploying Onboarding Server on JBoss / Wildfly](./Deploying-Wildfly.md).

## Supported Java Runtime Versions

The following Java runtime versions are supported:
- Java 17 (LTS release)

The Onboarding Server may run on other Java versions, however we do not perform extensive testing with non-LTS releases.


## Setting Up REST Service Authentication

Access to private integration REST services is limited to users with elevated privileges.
You can choose between Basic HTTP and OpenID Connect authentication, each requiring specific configurations as described below.
By default, basic HTTP authentication scheme is used.
To change this preference, set the `enrollment-server-onboarding.security.auth-type` configuration property.


### Basic HTTP Authentication

To use Basic HTTP authentication you have to specify the username and password using the following configuration properties:

For details, see the [configuration properties](./Configuration-Properties.md#basic-authentication) section


### OpenID Connect

To set up the OpenID Connect (OIDC) authentication, specify which authorization server to use using
the `spring.security.oauth2.resource-server.jwt.issuer-uri` configuration property.

Each request that requires privileged access must include JWT-encoded bearer token in the authorization header. The
passed token must include roles claim, containing a list of roles for which is the user authorized.

For additional configuration options, refer to the [configuration properties](./Configuration-Properties.md#openid-connect) section.
