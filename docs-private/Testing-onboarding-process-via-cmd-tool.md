# Testing oboarding process via cmd tool


List of cmd commands for local environment

## Create application

```shell
curl -s -H "Content-Type: application/json" -X POST -d '{ "requestObject": { "applicationId": "MR-TEST-APP" } }' http://localhost:8080/powerauth-java-server/rest/v3/application/create | json_pp
```

create sdk_config.json

```shell
curl -s -H "Content-Type: application/json" -X POST -d '{ "requestObject": { "applicationId": "MR-TEST-APP" } }' http://localhost:8080/powerauth-java-server/rest/v3/application/detail | json_pp
```

## Start onboarding process

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8083/enrollment-server-onboarding/api/onboarding/start" \
    --base-url "http://localhost:8081/enrollment-server" \
    --config-file "./sdk_config.json" \
    --status-file "./device_status.json" \
    --method "encrypt" \
    --data-file "./onboarding/1_start.json" \
    --scope "application" \
    --version "3.3" \
    --password "1234"
```

Request body:

```json
{
  "requestObject": {
    "identification": {
        "clientNumber": "dummy-user",
        "birthDate": "1970-03-21",
        "shouldFail": false
    }
  }
}
```

## Get OTP

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8083/enrollment-server-onboarding/api/onboarding/otp/detail" \
    --base-url "http://localhost:8081/enrollment-server" \
    --config-file "./sdk_config.json" \
    --status-file "./device_status.json" \
    --method "encrypt" \
    --data-file "./onboarding/2_otpDetail.json" \
    --scope "application" \
    --version "3.3" \
    --password "1234"
```

request body:

```json
{
  "requestObject": {
    "processId": "d9f92351-51b6-47e2-82b7-76ae02c63091",
    "otpType": "ACTIVATION"
  }
}
```


## Create activation

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8081/enrollment-server" \
    --config-file "./sdk_config.json" \
    --status-file "./device_status.json" \
    --identity-file "./onboarding/3_activationCreate_identityAttributes.json" \
    --method "create-custom" \
    --version "3.3" \
    --password "1234"
```

Request body:

```json
{
    "processId": "d9f92351-51b6-47e2-82b7-76ae02c63091",
    "otpCode": "05998958",
    "credentialsType": "ONBOARDING"
}
```


## Get consent text

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8083/enrollment-server-onboarding/api/identity/consent/text" \
    --base-url "http://localhost:8081/enrollment-server" \
    --status-file "./device_status.json" \
    --config-file "./sdk_config.json" \
    --method "validate-token" \
    --http-method "POST" \
    --resource-id "/api/identity/consent/text" \
    --data-file "./onboarding/9_consentText.json" \
    --version "3.3" \
    --token-id "bac3a94c-21c1-40c0-9684-50c711026ea5" \
    --token-secret "XBvTUDULLPehlw0WhxWRpg==" \
    --password "1234"
```

Request body:

```json
{
  "requestObject": {
    "processId": "6b5cd219-4330-457e-84ca-5a7c96aa2d98",
    "consentType": "GDPR"
  }
}
```

## Approve consent

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8083/enrollment-server-onboarding/api/identity/consent/approve" \
    --base-url "http://localhost:8081/enrollment-server" \
    --status-file "./device_status.json" \
    --config-file "./sdk_config.json" \
    --method "sign" \
    --http-method "POST" \
    --resource-id "/api/identity/consent/approve" \
    --signature-type "possession" \
    --data-file "./onboarding/10_consentApprove.json" \
    --version "3.3" \
    --password "1234"
```

Request body:

```json
{
  "requestObject": {
    "processId": "6b5cd219-4330-457e-84ca-5a7c96aa2d98",
    "consentType": "GDPR",
    "approved": true
  }
}
```

## Init process

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8083/enrollment-server-onboarding/api/identity/init" \
    --base-url "http://localhost:8081/enrollment-server" \
    --status-file "./device_status.json" \
    --config-file "./sdk_config.json" \
    --method "sign" \
    --http-method "POST" \
    --resource-id "/api/identity/init" \
    --signature-type "possession" \
    --data-file "./onboarding/4_init.json" \
    --version "3.3" \
    --password "1234"
```

Request body:

```json
{
  "requestObject": {
    "processId": "d9f92351-51b6-47e2-82b7-76ae02c63091"
  }
}
```


## Init SDK

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8083/enrollment-server-onboarding/api/identity/document/init-sdk" \
    --base-url "http://localhost:8081/enrollment-server" \
    --status-file "./device_status.json" \
    --config-file "./sdk_config.json" \
    --method "sign-encrypt" \
    --http-method "POST" \
    --resource-id "/api/identity/document/init-sdk" \
    --signature-type "possession" \
    --data-file "./onboarding/5_initSdk.json" \
    --version "3.3" \
    --password "1234"
```

Request body:

```json
{
  "requestObject": {
    "processId": "d9f92351-51b6-47e2-82b7-76ae02c63091",
    "attributes": {
      "platform": "android"
    }
  }
}
```


## Generate token

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8081/enrollment-server" \
    --status-file "./device_status.json" \
    --config-file "./sdk_config.json" \
    --method "create-token" \
    --signature-type "possession" \
    --version "3.3" \
    --password "1234"
```


## Submit documents

```shell
java -jar powerauth-java-cmd-1.10.0.jar \
    --url "http://localhost:8083/enrollment-server-onboarding/api/identity/document/submit" \
    --base-url "http://localhost:8081/enrollment-server" \
    --status-file "./device_status.json" \
    --config-file "./sdk_config.json" \
    --method "token-encrypt" \
    --http-method "POST" \
    --resource-id "/api/identity/document/submit" \
    --data-file "./onboarding/6_documentsSubmit.json" \
    --version "3.3" \
    --token-id "4c68d4a6-5b54-496d-8ba3-b7ff0a70cab2" \
    --token-secret "GJkuxwG9Ao07IT445gyvcA==" \
    --password "1234"
```

Request body:

```json
{
  "requestObject": {
    "processId": "d9f92351-51b6-47e2-82b7-76ae02c63091",
    "data": "zip_with_all_documents_as_base64",
    "resubmit": false,
    "documents": [
      {
        "filename": "id_front.jpg",
        "type": "ID_CARD",
        "side": "FRONT"
      },
      {
        "filename": "id_back.jpg",
        "type": "ID_CARD",
        "side": "BACK"
      }
    ]
  }
}
```
