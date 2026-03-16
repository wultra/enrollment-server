#!/bin/bash

mydir=`dirname $0`
cd $mydir/..

java -cp "target/dependency/*:target/*" \
     wiremock.Run \
    --port 8090 \
    --root-dir "src/test/wiremock" \
    --verbose \
    --global-response-templating \
    --extensions com.wultra.app.enrollmentserver.mockutils.OtpStoreTransformer
