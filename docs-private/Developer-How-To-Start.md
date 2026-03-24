# Developer - How to Start Guide

## General prerequisites

Following are _minimal_ versions of the tools and technologies used in the development
of the Enrollment Server project. You can use higher versions, but make sure to check
the compatibility.

* _JDK_ version 21.x
* _Maven_ version 3.9.x
* _PostgreSQL_ version 18.x
* _Liquibase_ version 4.33.x


## Enrollment Server

### Build

From the repository root, build all modules with:

```shell
mvn clean install
```

To build only the Enrollment Server module and its dependencies from the repository root, use:

```shell
mvn -pl enrollment-server -am clean install
```


### Database

* The default DB for development is _PostgreSQL_.
* Database changes are driven by Liquibase.

#### Set up

Ensure you have a database installed and running, and that you have an admin account.

##### Create a user and a database

Start a `psql` session with your superuser:

```shell
psql -U $(whoami) -d postgres
```

Then run following commands in the `psql` shell:

```sql
CREATE USER powerauth;
CREATE DATABASE powerauth OWNER powerauth;
```

By default, local development in this repository uses the `powerauth` user without a password.
If your local PostgreSQL setup requires password authentication, set a password for the user
and update the matching datasource and Liquibase settings in the commands below.

##### Load the data with Liquibase

This is an example how to invoke Liquibase.
Important and fixed parameter is `changelog-file`.
Others (like URL, username, password) depend on your environment.

To list all undeployed changesets run this `status` command.

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --url=jdbc:postgresql://localhost:5432/powerauth --username=powerauth status
```

To apply the changesets run this `update` command.

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --url=jdbc:postgresql://localhost:5432/powerauth --username=powerauth update
```


### Configure

For local development, the provided IntelliJ IDEA run configuration uses the `dev`
Spring profile together with `enrollment-server/src/main/resources/application-dev.properties`.

Outside this local `dev` setup, the default application configuration enables the `ext`
profile, so you can override values using `application-ext.properties` or environment
variables.

Common properties to review for local development:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/powerauth
spring.datasource.username=powerauth
spring.datasource.password=
powerauth.service.url=http://localhost:8080/powerauth-java-server/rest
powerauth.push.service.url=http://localhost:8081/powerauth-push-server
enrollment-server.auth-type=BASIC_HTTP
enrollment-server.admin.enabled=true
```

For additional details, see:

* [Configuration Properties](../docs/Configuration-Properties.md)
* [Deploying Enrollment Server](../docs/Deploying-Enrollment-Server.md)


### Run

The working directory is `enrollment-server`.

#### CLI

```shell
java -jar target/enrollment-server-x.y.z.war --spring.profiles.active=dev
```

The exact WAR filename can be found in the `target/` directory.

#### Maven

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### IntelliJ IDEA

* Use IntelliJ IDEA run configuration at `../.run/EnrollmentServerApplication.run.xml`
* The provided run configuration starts the server on `http://localhost:8081/enrollment-server`


### Smoke test

If you use the provided IntelliJ IDEA run configuration, run:

```shell
curl -v http://localhost:8081/enrollment-server/actuator/health
```

If you run the server directly from CLI or Maven without extra server parameters, run:

```shell
curl -v http://localhost:8080/actuator/health
```

Standalone CLI and Maven runs use port `8080` by default. Because Enrollment Server
Onboarding also uses `8080` by default, you can run only one of them at a time unless
you override `server.port` (and optionally `server.servlet.context-path`).

You should get response: `200 {"status":"UP"}`

You can check other APIs on:

* http://localhost:8080/swagger-ui/index.html


### Schema Diagram

For database structure overview, see:

* [Database Structure](../docs/Database-Structure.md)

#### Generate SQL script (optional)

##### PostgreSQL

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --output-file=./docs/sql/postgresql/generated-postgresql-script.sql updateSQL --url=offline:postgresql
```

##### Oracle

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --output-file=./docs/sql/oracle/generated-oracle-script.sql updateSQL --url=offline:oracle
```

##### MS SQL

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --output-file=./docs/sql/mssql/generated-mssql-script.sql updateSQL --url=offline:mssql
```


### Prepare environment variables

* Copy `deploy/env.list.tmp` to `./env.list` and edit the values to use it via `docker run --env-file env.list enrollment-server:1.9.0`
* Or set environment variables via `docker run -e ENROLLMENT_SERVER_DATASOURCE_USERNAME='powerauth' enrollment-server:1.9.0`


### Docker

#### Build the docker image

```shell
docker build . -t enrollment-server:1.9.0
```

#### Run the docker image

```shell
docker run -p 80:8080 -e ENROLLMENT_SERVER_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/powerauth' -e ENROLLMENT_SERVER_DATASOURCE_USERNAME='powerauth' -e ENROLLMENT_SERVER_DATASOURCE_PASSWORD='' enrollment-server:1.9.0
```


## Enrollment Server Onboarding

### Build

From the repository root, build all modules with:

```shell
mvn clean install
```

To build only the Enrollment Server Onboarding module and its dependencies from the repository root, use:

```shell
mvn -pl enrollment-server-onboarding -am clean install
```


### Database

* The default DB for development is _PostgreSQL_.
* Database changes are driven by Liquibase.
* If you already created the `powerauth` database and user for Enrollment Server, you can reuse them here.

#### Set up

If you have not created the database and user yet, use the same PostgreSQL setup as in the Enrollment Server section.
See [Database](#database) section above for details.

##### Load the data with Liquibase

This is an example how to invoke Liquibase.
Important and fixed parameter is `changelog-file`.
Others (like URL, username, password) depend on your environment.

To list all undeployed changesets run this `status` command.

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --url=jdbc:postgresql://localhost:5432/powerauth --username=powerauth status
```

To apply the changesets run this `update` command.

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --url=jdbc:postgresql://localhost:5432/powerauth --username=powerauth update
```


### Configure

For local development, the provided IntelliJ IDEA run configuration uses the `dev`
Spring profile together with `enrollment-server-onboarding/src/main/resources/application-dev.properties`.

Outside this local `dev` setup, the default application configuration enables the `ext`
profile, so you can override values using `application-ext.properties` or environment
variables.

Common properties to review for local development:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/powerauth
spring.datasource.username=powerauth
spring.datasource.password=
powerauth.service.url=http://localhost:8080/powerauth-java-server/rest
enrollment-server-onboarding.security.auth-type=BASIC_AUTH
enrollment-server-onboarding.identity-verification.enabled=true
enrollment-server-onboarding.document-verification.provider=mock
```

For additional details, see:

* [Configuration Properties](../docs/onboarding/Configuration-Properties.md)
* [Deploying Onboarding Server](../docs/onboarding/Deploying-Onboarding-Server.md)


### Run

The working directory is `enrollment-server-onboarding`.

#### CLI

```shell
java -jar target/enrollment-server-onboarding-x.y.z.war --spring.profiles.active=dev
```

The exact WAR filename can be found in the `target/` directory.

#### Maven

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### IntelliJ IDEA

* Use IntelliJ IDEA run configuration at `../.run/EnrollmentServerOnboardingApplication.run.xml`
* The provided run configuration starts the server on `http://localhost:8083/enrollment-server-onboarding`


### Smoke test

If you use the provided IntelliJ IDEA run configuration, run:

```shell
curl -v http://localhost:8083/enrollment-server-onboarding/actuator/health
```

If you run the server directly from CLI or Maven without extra server parameters, run:

```shell
curl -v http://localhost:8080/actuator/health
```

Standalone CLI and Maven runs use port `8080` by default. Because Enrollment Server
also uses `8080` by default, you can run only one of them at a time unless you
override `server.port` (and optionally `server.servlet.context-path`).

You should get response: `200 {"status":"UP"}`

You can check other APIs on:

* http://localhost:8080/swagger-ui/index.html


### Schema Diagram

For database structure overview, see:

* [Database Structure](../docs/onboarding/Database-Structure.md)

#### Generate SQL script (optional)

##### Oracle

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --output-file=./docs/sql/oracle/generated-oracle-script.sql updateSQL --url=offline:oracle
```

##### MS SQL

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --output-file=./docs/sql/mssql/generated-mssql-script.sql updateSQL --url=offline:mssql
```

##### PostgreSQL

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --output-file=./docs/sql/postgresql/generated-postgresql-script.sql updateSQL --url=offline:postgresql
```
