#!/bin/bash

mydir=`dirname $0`
cd $mydir/..

# Liveness check proxy configuration (all optional). When LIVENESS_CHECK_PROXY_BASE_URL is set,
# requests to POST /process/event are forwarded to ${LIVENESS_CHECK_PROXY_BASE_URL}/onboarding-events,
# otherwise a static 200 response (body-process-event.json) is returned.
# export LIVENESS_CHECK_PROXY_BASE_URL="http://localhost:8092/liveness-check-proxy"
# export LIVENESS_CHECK_PROXY_USERNAME="admin"
# export LIVENESS_CHECK_PROXY_PASSWORD="password"

java -cp "target/dependency/*:target/*" \
     wiremock.Run \
    --port 8090 \
    --root-dir "src/test/wiremock" \
    --verbose \
    --global-response-templating \
    --extensions com.wultra.app.enrollmentserver.mockutils.OtpStoreTransformer,com.wultra.app.enrollmentserver.mockutils.ProcessEventProxyTransformer
