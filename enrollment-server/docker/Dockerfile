FROM ghcr.io/wultra/wultra-base-images:jre-semeru-25_2

# Prepare environment variables
ENV LOGBACK_CONF=/opt/logback/conf \
    APP_PATH="/app/enrollment-server.war" \
    CONTEXT_PATH="/enrollment-server"

# Deploy and run applications
COPY ./enrollment-server/target/enrollment-server*.war "${APP_PATH}"

COPY enrollment-server/docker/conf/logback/* $LOGBACK_CONF/