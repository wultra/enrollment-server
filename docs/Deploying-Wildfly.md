# Deploying Enrollment Server on JBoss / Wildfly

## JBoss Deployment Descriptor 

Enrollment Server contains the following configuration in `jboss-deployment-structure.xml` file for JBoss:

```
<?xml version="1.0"?>
<jboss-deployment-structure xmlns="urn:jboss:deployment-structure:1.2">
	<deployment>
		<exclude-subsystems>
			<!-- disable the logging subsystem because the application manages its own logging independently -->
			<subsystem name="logging" />
		</exclude-subsystems>

		<resources>
			<!-- use WAR provided Bouncy Castle -->
			<resource-root path="WEB-INF/lib/bcprov-jdk15on-${BC_VERSION}.jar" use-physical-code-source="true"/>
		</resources>

		<dependencies>
			<module name="com.wultra.powerauth.enrollment-server.conf" />
		</dependencies>
		<local-last value="true" />
	</deployment>
</jboss-deployment-structure>
```

The deployment descriptor requires configuration of the `com.wultra.powerauth.enrollment-server.conf` module.

## JBoss Module for Enrollment Server Configuration

Create a new module in `PATH_TO_JBOSS/modules/system/layers/base/com/wultra/powerauth/enrollment-server/conf/main`.

The files described below should be added into this folder.

### Main Module Configuration

The `module.xml` configuration is used for module registration. It also adds resources from the module folder to classpath:
```
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.3" name="com.wultra.powerauth.enrollment-server.conf">
    <resources>
        <resource-root path="." />
    </resources>
</module>
```

### Logging Configuration

Use the `logback.xml` file to configure logging, for example:
```
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">

        <property name="LOG_FILE_DIR" value="/var/log/powerauth" />
        <property name="LOG_FILE_NAME" value="enrollment-server" />
        <property name="INSTANCE_ID" value="${jboss.server.name}" />

        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
                <file>${LOG_FILE_DIR}/${LOG_FILE_NAME}-${INSTANCE_ID}.log</file>
                <immediateFlush>true</immediateFlush>
                <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                        <fileNamePattern>${LOG_FILE_DIR}/${LOG_FILE_NAME}-${INSTANCE_ID}-%d{yyyy-MM-dd}-%i.log</fileNamePattern>
                        <maxFileSize>10MB</maxFileSize>
                        <maxHistory>5</maxHistory>
                        <totalSizeCap>100MB</totalSizeCap>
                </rollingPolicy>
                <encoder>
                        <charset>UTF-8</charset>
                        <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
                </encoder>
        </appender>

        <logger name="com.wultra" level="INFO" />
        <logger name="io.getlime" level="INFO" />

        <root level="INFO">
                <appender-ref ref="FILE" />
        </root>
</configuration>
```

### Application Configuration

The `application-ext.properties` file is used to override default configuration properties, for example:
```
# PowerAuth Client configuration
powerauth.service.url=http://[host]:[port]/powerauth-java-server/rest

# PowerAuth Push Server configuration
powerauth.push.service.url=http://[host]:[port]/powerauth-push-server
```

Enrollment Server Spring application uses the `ext` Spring profile which activates overriding of default properties by `application-ext.properties`.

### Bouncy Castle Installation

Since Enrollment Server in version `1.1.x`, installing the Bouncy Castle into Wildfy Server is no longer required. The latest version is bundled with the app and cryptographic primitives should work out of the box.
