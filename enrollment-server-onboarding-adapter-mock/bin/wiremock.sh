#!/bin/bash

mydir=`dirname $0`
cd $mydir/..

java -jar target/dependency/wiremock-jre8-standalone.jar \
    --port 8090 \
    --root-dir "src/test/wiremock" \
    --verbose \
    --global-response-templating
