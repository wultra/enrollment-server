FROM liquibase:4.33.0-alpine

LABEL maintainer="Wultra <hello@wultra.com>"

COPY enrollment-server-onboarding/docker-init/liquibase/data /liquibase/changelog
COPY enrollment-server-onboarding/docker-init/docker-entrypoint.sh /docker-entrypoint.sh

USER liquibase
ENTRYPOINT ["/docker-entrypoint.sh"]
