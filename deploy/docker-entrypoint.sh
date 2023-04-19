#!/usr/bin/env sh

liquibase --headless=true --log-level=INFO --changeLogFile=$LB_HOME/db/changelog/changesets/enrollment-server/db.changelog-module.xml \
  --username=$ENROLLMENT_SERVER_DATASOURCE_USERNAME \
  --password=$ENROLLMENT_SERVER_DATASOURCE_PASSWORD \
  --url=$ENROLLMENT_SERVER_DATASOURCE_URL \
  update

catalina.sh run
