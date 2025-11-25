# Onboarding Adapter Mock

Mock of onboarding adapter using [WireMock].

## Run via Docker

Build the docker image:
```bash
docker build -t enrollment-server-onboarding-adapter-mock:latest -f docker/Dockerfile .
```

Run the image:
```bash
docker run -p 8888:8080 enrollment-server-onboarding-adapter-mock:latest
```
adjust the host port (i.e.8888) per your preference

## Run via Maven

**NOTE**: Before the first usage of the standalone [WireMock] please execute the following command:

```bash
mvn clean package
```

You can start [WireMock] using the predefined script:

```bash
./bin/wiremock.sh
```

[WireMock]: https://wiremock.org/docs/)
