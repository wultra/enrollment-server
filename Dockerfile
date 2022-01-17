FROM tomcat:jdk11-adoptopenjdk-openj9
LABEL maintainer="petr@wultra.com"

# Prepare environment variables
ENV JAVA_HOME /opt/java/openjdk
ENV TOMCAT_HOME /usr/local/tomcat
ENV WAR_VERSION 1.2.0

# Clear root context
RUN rm -rf $TOMCAT_HOME/webapps/*

# Add valve for proxy with SSL termination
RUN sed -i 's/<\/Host>/<Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="X-Forwarded-For" protocolHeader="X-Forwarded-Proto"\/><\/Host>/' $TOMCAT_HOME/conf/server.xml

# Deploy and run applications
COPY docker/enrollment-server.xml $TOMCAT_HOME/conf/Catalina/localhost/
COPY target/enrollment-server-$WAR_VERSION.war $TOMCAT_HOME/webapps/enrollment-server.war

# Create user tomcat and run Tomcat under this user
RUN groupadd -r tomcat
RUN useradd -r -g tomcat -d $TOMCAT_HOME -s /sbin/nologin tomcat
RUN chown -R tomcat:tomcat $TOMCAT_HOME

USER tomcat
CMD ["catalina.sh", "run"]
