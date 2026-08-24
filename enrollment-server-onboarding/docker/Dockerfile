FROM ghcr.io/wultra/wultra-base-images:jre-semeru-25_3

# Prepare environment variables
ENV LOGBACK_CONF=/opt/logback/conf \
    APP_PATH="/app/enrollment-server-onboarding.war" \
    CONTEXT_PATH="/enrollment-server-onboarding"

# Deploy and run applications
COPY ./enrollment-server-onboarding/target/enrollment-server-onboarding*.war "${APP_PATH}"

COPY enrollment-server-onboarding/docker/conf/logback/* $LOGBACK_CONF/