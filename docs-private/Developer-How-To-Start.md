# Developer - How to Start Guide

## General prerequisites

Following are _minimal_ versions of the tools and technologies used in the development
of the PowerAuth Server. You can use higher versions, but make sure to check the compatibility.

* _JDK_ version 21.x
* _Maven_ version 3.9.x
* _PostgreSQL_ version 18.x
* _Liquibase_ version 4.33.x


## Enrollment Server

### Build

Build with:

```shell
mvn clean install
```


### Database

* The default DB for development is _PostgreSQL_.
* Database changes are driven by Liquibase.

#### Set up

Ensure you have a database installed and running, and that you have an admin account.

##### Create a user and a schema

Start a `psql` session with your superuser:

```shell
psql -U $(whoami) -d postgres
```

Then run following commands in the `psql` shell:

```sql
CREATE USER powerauth WITH PASSWORD 'powerauth';
CREATE DATABASE powerauth OWNER powerauth;
```

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


#### Generate SQL script (optional)

##### PostgreSQL

```shell
liquibase --changeLogFile=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --output-file=./docs/sql/postgresql/generated-postgresql-script.sql updateSQL --url=offline:postgresql
```

##### Oracle

```shell
liquibase --changeLogFile=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --output-file=./docs/sql/oracle/generated-oracle-script.sql updateSQL --url=offline:oracle
```

##### MS SQL

```shell
liquibase --changeLogFile=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --output-file=./docs/sql/mssql/generated-mssql-script.sql updateSQL --url=offline:mssql
```


### Prepare environment variables

* Copy `deploy/env.list.tmp` to `./env.list` and edit the values to use it via `docker run --env-file env.list enrollment-server:1.9.0`
* Or set environment variables via `docker run -e ENROLLMENT_SERVER_DATASOURCE_USERNAME='powerauth' enrollment-server:1.9.0`


### Run

The working directory is `enrollment-server`.

#### CLI

```shell
java -jar target/enrollment-server-x.y.z.war
```

#### Maven

```shell
mvn spring-boot:run
```

#### IntelliJ Idea

* Use IntelliJ Idea run configuration at `../.run/EnrollmentServerApplication.run.xml`
* Open [http://localhost:8081/enrollment-server/actuator/health](http://localhost:8081/enrollment-server/actuator/health) and you should get `{"status":"UP"}`

### Smoke test

Run following `curl` command:

```shell
curl -v http://localhost:8080/actuator/health
```

You should get response: `200 {"status":"UP"}`

You can check other APIs on:

* http://localhost:8080/swagger-ui/index.html


### Docker

### Build the docker image

```shell
docker build . -t enrollment-server:1.9.0
```



### Run the docker image

```shell
docker run -p 80:8080 -e ENROLLMENT_SERVER_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/powerauth' -e ENROLLMENT_SERVER_DATASOURCE_USERNAME='powerauth' -e ENROLLMENT_SERVER_DATASOURCE_PASSWORD='' enrollment-server:1.9.0
```


## Enrollment Server Onboarding


### Standalone Run

- Use IntelliJ Idea run configuration at `../.run/EnrollmentServerOnboardingApplication.run.xml`
- Open [http://localhost:8083/enrollment-server-onboarding/actuator/health](http://localhost:8083/enrollment-server-onboarding/actuator/health) and you should get `{"status":"UP"}`


### Database

Database changes are driven by Liquibase.

This is an example how to manually check the Liquibase status.
Important and fixed parameter is `changelog-file`.
Others (like URL, username, password) depend on your environment.

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --url=jdbc:postgresql://localhost:5432/powerauth --username=powerauth status
``` 

To generate SQL script run this command.


#### Oracle

```shell
liquibase --changeLogFile=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --output-file=./docs/sql/oracle/generated-oracle-script.sql updateSQL --url=offline:oracle
```


#### MS SQL

```shell
liquibase --changeLogFile=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --output-file=./docs/sql/mssql/generated-mssql-script.sql updateSQL --url=offline:mssql
```


#### PostgreSQL

```shell
liquibase --changeLogFile=./docs/db/changelog/changesets/enrollment-server-onboarding/db.changelog-module.xml --output-file=./docs/sql/postgresql/generated-postgresql-script.sql updateSQL --url=offline:postgresql
```
