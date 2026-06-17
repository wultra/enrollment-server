FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

# Both the root POM and the module POM are required so Maven can resolve the parent
COPY pom.xml pom.xml
COPY enrollment-server-onboarding-adapter-mock/pom.xml enrollment-server-onboarding-adapter-mock/pom.xml
COPY enrollment-server-onboarding-adapter-mock/src enrollment-server-onboarding-adapter-mock/src

RUN  --mount=type=cache,target=/root/.m2 \
    cd enrollment-server-onboarding-adapter-mock \
    && mvn -DskipTests -Dmaven.javadoc.skip=true package \
	&& cp target/enrollment-server-onboarding-adapter-mock*.jar /tmp/ \
    && cp target/dependency/jackson-*.jar /tmp

FROM wiremock/wiremock:latest

COPY enrollment-server-onboarding-adapter-mock/src/test/wiremock /home/wiremock
COPY --from=build /tmp/*.jar /var/wiremock/extensions

ENTRYPOINT ["/docker-entrypoint.sh", "--global-response-templating", "--disable-gzip", "--verbose", "--extensions", "com.wultra.app.enrollmentserver.mockutils.OtpStoreTransformer,com.wultra.app.enrollmentserver.mockutils.ProcessEventProxyTransformer"]
