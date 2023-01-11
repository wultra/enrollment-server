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
logging.pattern.console=%clr(%d{${LOG_DATEFORMAT_PATTERN:yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:%5p}) [%X{X-Correlation-ID}] %clr(%5p) %clr(${PID: }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:%wEx}
```

Update the correlation header name in case you want to use a different HTTP header than `X-Correlation-ID`. You can also update the regular expression for correlation header value validation to match the exact format of correlation header value that will be used.

The logging pattern for console is the Spring default logging pattern with the addition of `%X{X-Correlation-ID}`. This variable is used to log the actual value of the correlation header.

For best traceability, the correlation headers should be enabled in the whole PowerAuth stack, so enable the correlation headers in other deployed applications, too. The configuration property names are the same in all PowerAuth applications: `powerauth.service.correlation-header.*`. The correlation header values are passed through the stack, so the requests can be traced easily across multiple components.

## Deploying Onboarding Server On JBoss / Wildfly

Follow the extra instructions in chapter [Deploying Onboarding Server on JBoss / Wildfly](./Deploying-Wildfly.md).

## Supported Java Runtime Versions

The following Java runtime versions are supported:
- Java 11 (LTS release)
- Java 17 (LTS release)

The Onboarding Server may run on other Java versions, however we do not perform extensive testing with non-LTS releases.
