FROM wiremock/wiremock:latest
COPY src/test/wiremock /home/wiremock
ENTRYPOINT ["/docker-entrypoint.sh", "--global-response-templating", "--disable-gzip", "--verbose"]