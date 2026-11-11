FROM ghcr.io/wultra/wultra-base-images:jre-semeru-25_7@sha256:30d496462a9fa1ead309d9397f23a27f6deadf235733547e7c24a4040a3f98fa

# Prepare environment variables
ENV LOGBACK_CONF=/opt/logback/conf \
    APP_PATH="/app/enrollment-server-onboarding.war" \
    CONTEXT_PATH="/enrollment-server-onboarding"

# Deploy and run applications
COPY ./enrollment-server-onboarding/target/enrollment-server-onboarding*.war "${APP_PATH}"

COPY enrollment-server-onboarding/docker/conf/logback/* $LOGBACK_CONF/