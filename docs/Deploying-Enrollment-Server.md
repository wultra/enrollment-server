# Deploying Enrollment Server

This chapter explains how to deploy Enrollment Server.

<!-- begin box warning -->
The enrollment server component will need to be customized in case you need to customize the activation process. The customization is described in the tutorial [Implementing the Server-Side for Authentication in Mobile Banking Apps (SCA)](https://developers.wultra.com/products/mobile-token/2021-05/tutorials/Authentication-in-Mobile-Apps/Server-Side-Tutorial#deploying-the-enrollment-server).
<!-- end -->

## Downloading Enrollment Server

You can download the latest `enrollment-server.war` from the [Enrollment Server releases page](https://github.com/wultra/enrollment-server/releases).

## Configuring Enrollment Server

The default implementation of an Enrollment Server has only one compulsory configuration parameter `powerauth.service.url` that configures the REST service location of a PowerAuth Server. The default value for this property points to `localhost`:

```bash
powerauth.service.url=http://localhost:8080/powerauth-java-server/rest
```

## Configuration of Enrollment Server Functionality

Publishing of Mobile Token endpoints can be enabled or disabled using following configuration property:
```bash
enrollment-server.mtoken.enabled=true
```
The activation spawn functionality can be enabled or disabled using following configuration property:
```bash
enrollment-server.activation-spawn.enabled=false
```

## Setting Up REST Service Credentials

_(optional)_ In case PowerAuth Server uses a [restricted access flag in the server configuration](https://github.com/wultra/powerauth-server/blob/develop/docs/Deploying-PowerAuth-Server.md#enabling-powerauth-server-security), you need to configure credentials for the Enrollment Server so that it can connect to the REST service:

```sh
powerauth.service.security.clientToken=
powerauth.service.security.clientSecret=
```

<!-- begin box info -->
The RESTful interface is secured using Basic HTTP Authentication (pre-emptive). The credentials are stored in the `pa_integration` table.
<!-- end -->

## Configuring Push Server

The Enrollment Server also allows simple device registration to push notifications by calling PowerAuth Push Server API. While configuring this URL is technically optional, we recommend configuring the push server URL:

```bash
powerauth.push.service.url=http://localhost:8080/powerauth-push-server
```

## Deploying Enrollment Server

You can deploy Enrollment Server into any Java EE container.

The default configuration works best with Apache Tomcat server running on default port 8080. In this case, the deployed server is accessible on `http://localhost:8080/enrollment-server/`.

To deploy Enrollment Server to Apache Tomcat, simply copy the WAR file in your `webapps` folder or deploy it using the "Tomcat Web Application Manager" application (usually deployed on default Tomcat address `http://localhost:8080/manager`).

Running Enrollment Server from console using the `java -jar` command is not supported.

## Correlation Header Configuration (Optional)

You can enable correlation header logging in Enrollment server by enabling the following properties:

```properties
powerauth.service.correlation-header.enabled=true
powerauth.service.correlation-header.name=X-Correlation-ID
powerauth.service.correlation-header.value.validation-regexp=[a-zA-Z0-9\\-]{8,1024}
logging.pattern.console=%clr(%d{${LOG_DATEFORMAT_PATTERN:yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:%5p}) [%X{X-Correlation-ID}] %clr(%5p) %clr(${PID: }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:%wEx}
```

Update the correlation header name in case you want to use a different HTTP header than `X-Correlation-ID`. You can also update the regular expression for correlation header value validation to match the exact format of correlation header value that will be used.

The logging pattern for console is the Spring default logging pattern with the addition of `%X{X-Correlation-ID}`. This variable is used to log the actual value of the correlation header.

For best traceability, the correlation headers should be enabled in the whole PowerAuth stack, so enable the correlation headers in other deployed applications, too. The configuration property names are the same in all PowerAuth applications: `powerauth.service.correlation-header.*`. The correlation header values are passed through the stack, so the requests can be traced easily across multiple components.

## Deploying Enrollment Server On JBoss / Wildfly

Follow the extra instructions in chapter [Deploying Enrollment Server on JBoss / Wildfly](./Deploying-Wildfly.md).

## Supported Java Runtime Versions

The following Java runtime versions are supported:
- Java 11 (LTS release)
- Java 17 (LTS release)

The Enrollment Server may run on other Java versions, however we do not perform extensive testing with non-LTS releases.

## How to Disable Display of Tomcat Version

It case you do not want to show Tomcat version on error pages when deploying Enrollment server, you can use the following configuration:

- Edit the file `<install-directory>/conf/server.xml`.
- Search for the parameters `<Host name="..."/>`.
- Just below that line, insert the following parameters `<Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/>`.
- Restart Tomcat.
