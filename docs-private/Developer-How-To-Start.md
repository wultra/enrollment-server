# Developer - How to Start Guide


## Enrollment Server


### Standalone Run

- Enable maven profile `standalone`
- Use IntelliJ Idea run configuration at `../.run/EnrollmentServerApplication.run.xml`
- Open [http://localhost:8081/enrollment-server/actuator/health](http://localhost:8081/enrollment-server/actuator/health) and you should get `{"status":"UP"}`


### Database

Database changes are driven by Liquibase.

This is an example how to manually check the Liquibase status.
Important and fixed parameter is `changelog-file`.
Others (like URL, username, password) depend on your environment.

```shell
liquibase --changelog-file=./docs/db/changelog/changesets/enrollment-server/db.changelog-module.xml --url=jdbc:postgresql://localhost:5432/powerauth --username=powerauth status
```


### Docker


### Build War

```shell
mvn clean package
```


### Build the docker image

```shell
docker build . -t enrollment-server:1.5.0
```


### Prepare environment variables

* Copy `deploy/env.list.tmp` to `./env.list` and edit the values to use it via `docker run --env-file env.list IMAGE`
* Or set environment variables via `docker run -e ENROLLMENT_SERVER_DATASOURCE_USERNAME='powerauth' IMAGE`


### Run the docker image

```shell
docker run -p 80:8080 -e ENROLLMENT_SERVER_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/powerauth' -e ENROLLMENT_SERVER_DATASOURCE_USERNAME='powerauth' -e ENROLLMENT_SERVER_DATASOURCE_PASSWORD='' enrollment-server:1.5.0 
```


## Enrollment Server Onboarding


### Standalone Run

- Enable maven profile `standalone`
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
