# Deploying Enrollment Server

This chapter explains how to deploy Enrollment Server.

## Downloading Enrollment Server

You can download the latest `enrollment-server.war` from the [Enrollment Server releases page](https://github.com/wultra/enrollment-server/releases).

## Configuring Enrollment Server

The default implementation of a Enrollment Server has only one compulsory configuration parameter `powerauth.service.url` that configures the SOAP endpoint location of a PowerAuth Server. The default value for this property points to `localhost`:

```bash
powerauth.service.url=http://localhost:8080/powerauth-java-server/soap
```

## Setting Up SOAP Service Credentials

_(optional)_ In case Enrollment Server uses a [restricted access flag in the server configuration](https://github.com/wultra/powerauth-server/blob/develop/docs/Deploying-PowerAuth-Server.md#enabling-powerauth-server-security), you need to configure credentials for the Enrollment Server so that it can connect to the SOAP service:

```sh
powerauth.service.security.clientToken=
powerauth.service.security.clientSecret=
```

## Configuring Push Server

The Enrollment Server also allows simple device registration to push notifications by calling PowerAuth Push Server API. While configuring this URL is technically optional, it is strongly suggested to configure the push server URL:

```bash
powerauth.push.service.url=http://localhost:8080/powerauth-push-server/
```

## Deploying Enrollment Server

You can deploy Enrollment Server into any Java EE container.

The default configuration works best with Apache Tomcat server running on default port 8080. In this case, the deployed server is accessible on `http://localhost:8080/enrollment-server/`.

To deploy Enrollment Server to Apache Tomcat, simply copy the WAR file in your `webapps` folder or deploy it using the "Tomcat Web Application Manager" application (usually deployed on default Tomcat address `http://localhost:8080/manager`).

## Deploying Enrollment Server On JBoss / Wildfly

Follow the extra instructions in chapter [Deploying Enrollment Server on JBoss / Wildfly](./Deploying-Wildfly.md).
