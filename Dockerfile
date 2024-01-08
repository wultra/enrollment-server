FROM ibm-semeru-runtimes:open-17.0.9_9-jre
LABEL maintainer="petr@wultra.com"

# Prepare environment variables
ENV JAVA_HOME=/opt/java/openjdk \
    LB_HOME=/usr/local/liquibase \
    LB_VERSION=4.23.2 \
    PKG_RELEASE=1~jammy \
    TOMCAT_HOME=/usr/local/tomcat \
    TOMCAT_MAJOR=10 \
    TOMCAT_VERSION=10.1.17 \
    TZ=UTC

ENV PATH=$PATH:$LB_HOME:$TOMCAT_HOME/bin

# Init
RUN apt-get -y update  \
    && apt-get -y upgrade \
    && apt-get -y install bash curl wget

# Install tomcat
RUN curl -jkSL -o /tmp/apache-tomcat.tar.gz http://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_MAJOR}/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz \
    && [ "ff9670f9cd49a604e47edfbcfb5855fe59342048c3278ea8736276b51327adf2d076973f3ad1b8aa7870ef26c28cf7111527be810b445c9927f2a457795f5cb6  /tmp/apache-tomcat.tar.gz" = "$(sha512sum /tmp/apache-tomcat.tar.gz)" ] \
    && gunzip /tmp/apache-tomcat.tar.gz \
    && tar -C /opt -xf /tmp/apache-tomcat.tar \
    && ln -s /opt/apache-tomcat-$TOMCAT_VERSION $TOMCAT_HOME

# Clear root context
RUN rm -rf $TOMCAT_HOME/webapps/*

# This setup was inspired by https://github.com/mobtitude/liquibase/blob/master/Dockerfile
RUN set -x \
    && wget -q -O /tmp/liquibase.tar.gz "https://github.com/liquibase/liquibase/releases/download/v$LB_VERSION/liquibase-$LB_VERSION.tar.gz" \
    && [ "fc7d2a9fa97d91203d639b664715d40953c6c9155a5225a0ddc4c8079b9a3641  /tmp/liquibase.tar.gz" = "$(sha256sum /tmp/liquibase.tar.gz)" ] \
    && mkdir -p "$LB_HOME" \
    && tar -xzf /tmp/liquibase.tar.gz -C "$LB_HOME" \
    && rm -rf "$LB_HOME/sdk" \
# Uninstall packages which are no longer needed and clean apt caches
    && apt-get -y remove wget curl gettext-base \
    && apt-get -y purge --auto-remove \
    && rm -rf /tmp/* /var/cache/apt/*

# Liquibase - changesets
RUN rm -rf $LB_HOME/data
COPY docs/db/changelog $LB_HOME/db/changelog

# Add valve for proxy with SSL termination
RUN sed -i 's/<\/Host>/<Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="X-Forwarded-For" protocolHeader="X-Forwarded-Proto"\/><\/Host>/' $TOMCAT_HOME/conf/server.xml

# Deploy and run applications
COPY deploy/enrollment-server.xml $TOMCAT_HOME/conf/Catalina/localhost/
COPY enrollment-server/target/enrollment-server-*.war $TOMCAT_HOME/webapps/enrollment-server.war

# Add PowerAuth User
RUN groupadd -r powerauth \
    && useradd -r -g powerauth -s /sbin/nologin powerauth \
    && chown -R powerauth:powerauth $TOMCAT_HOME \
    && chown -R powerauth:powerauth /opt/apache-tomcat-$TOMCAT_VERSION

# Docker configuration
EXPOSE 8080
STOPSIGNAL SIGQUIT

USER powerauth

# Define entry point with mandatory commands
COPY deploy/docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
