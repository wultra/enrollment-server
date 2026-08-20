# Installation

The Enrollment Server Onboarding is distributed as two Docker images:

- `powerauth/enrollment-server-onboarding` – the application runtime
- `powerauth/enrollment-server-onboarding-init` – a lightweight init image that runs Liquibase database migrations

Use these images to deploy locally or on any cloud provider (Azure, AWS, ...).

## Pull the Docker Image

```sh
docker pull powerauth/enrollment-server-onboarding:${VERSION}
docker pull powerauth/enrollment-server-onboarding-init:${VERSION}
```

## Configure the Docker Images

After you pull the Docker images in your own container repository, you need to prepare the `env.list` file with all the environment variables required for container launch. You can use [Configuration Properties](./Configuration-Properties.md) to bootstrap the configuration.

<!-- begin box warning -->
**Set The Right Database URL**<br/>
The datasource URL for our Docker container follows the structure of the JDBC connectivity. Make sure to provide a valid JDBC URL to the configuration (starting with `jdbc:` prefix). Be especially careful when working on `localhost`! From the Docker container perspective, `localhost` is in the internal network. To connect to your host's localhost, use `host.docker.internal` host name.
<!-- end -->

You need to edit at least the properties below.

```
ENROLLMENT_SERVER_ONBOARDING_DATASOURCE_URL=jdbc:postgresql://db-server:5432/onboarding
ENROLLMENT_SERVER_ONBOARDING_DATASOURCE_USERNAME=$USERNAME$
ENROLLMENT_SERVER_ONBOARDING_DATASOURCE_PASSWORD=$PASSWORD$
ENROLLMENT_SERVER_ONBOARDING_POWERAUTH_SERVICE_URL=http://powerauth-server:8080/powerauth-java-server/rest
ENROLLMENT_SERVER_ONBOARDING_IPROOV_API_KEY
ENROLLMENT_SERVER_ONBOARDING_IPROOV_API_SECRET
ENROLLMENT_SERVER_ONBOARDING_IPROOV_SERVICE_BASE_URL
ENROLLMENT_SERVER_ONBOARDING_IPROOV_OAUTH_CLIENT_USERNAME
ENROLLMENT_SERVER_ONBOARDING_IPROOV_OAUTH_CLIENT_PASSWORD
```

Database schema creation and upgrades are handled by the separate init image via [Liquibase](https://www.liquibase.org/). The main application container does not perform database migrations.

The init image reuses the same properties configured above, so no additional variables need to be set for the init image:

```
ENROLLMENT_SERVER_ONBOARDING_DATASOURCE_URL
ENROLLMENT_SERVER_ONBOARDING_DATASOURCE_USERNAME
ENROLLMENT_SERVER_ONBOARDING_DATASOURCE_PASSWORD
```

## Initialize the Database Schema

Run the init image once to create or upgrade the database schema:

```sh
docker run --rm --env-file env.list \
    --name=enrollment-server-onboarding-init powerauth/enrollment-server-onboarding-init:${VERSION}
```
The command above runs Liquibase migrations and exits on success. If there are any issues connecting to the database or applying the migrations, the command will fail with an error message.

To keep the init container alive after the migration finishes (e.g. for Kubernetes init container health checks), set `KEEP_RUNNING=true` (optionally with `KEEP_RUNNING_PORT`, default `8080`).

## Start the Docker Container
After you prepare the configuration file, you can run the image using `docker run`:
```sh
docker run --env-file env.list -d -it -p 8080:8000 \
    --name=enrollment-server-onboarding powerauth/enrollment-server-onboarding:${VERSION}
```

This will launch the application container with the properties you specified. Make sure you ran the init image first to prepare the database schema.

<!-- begin box info -->
The Docker containers use the standard UTC timezone.
<!-- end -->
