# External Onboarding Services
<!-- template api -->

Onboarding Server can call services exposed by the entity to extend the KYC process. These services need to be implemented by the client, so the documentation below describes API requirements, not existing services.

<!-- begin remove -->
- `POST` [/client/approval](#anchor) - Onboarding Approval Service
- `POST` [/user/lookup](#anchor) - User Lookup Service
- `POST` [/client/evaluation](#anchor) - Client Evaluation Service
- `POST` [/otp/send](#anchor) - OTP Delivery Service
- `POST` [consent/storage](#anchor) - Consent Storage Service
- `POST` [/consent/text](#anchor) - Consent Text Service
<!-- end -->

## Authentication

Onboarding server optionally supports HTTP Basic Auth when calling external services.

```
Authorization: Basic <credentials>
```

If you choose to secure your API, credentials (`username` and `password`) have to be configured at onboarding server.

## API URL

When calling API, endpoint URL is constructed from configured "base path" and fixed "resource uri"

```
$BASE_PATH/$RESOURCE_URI
```

- `BASE_PATH` cointains protocol, domain and optionally port or prefixes (like "api" or "version" etc.)
- `RESOURCE_URI` containes fixed endpoint URI (see respective endpoint description)

Example API URL:

``` bash
https://my.great.api/api/v1/client/approval
```

For Onboarding Server configuration `BASE_PATH` value is needed.

## HTTP Headers

| Header                         | Optional | Description               |
|:-------------------------------|:---------|:--------------------------|
| Content-Type: application/json | false    | Defines request MIME type |
| X-Correlation-Id: < id >       | true     | Defines transaction ID    |
| X-Request-Id: < id >           | true     | Defines request ID        |

Optional headers can be configured to different names.

## Error Handling

Onboarding Server uses following format for error response body, accompanied by an appropriate HTTP status code. Besides the HTTP error codes that application server may return regardless of server application (such as 404 when resource is not found or 503 when server is down), following ERROR codes may be returned:

| Error Code         | HTTP Code | Description                                                |
|:-------------------|:----------|:-----------------------------------------------------------|
| ERROR_GENERIC      | 400       | Issue with a request format or issue of the business logic |
| ERROR_UNAUTHORIZED | 401       | Unauthorized request                                       |

All error responses that are produced by the Onboarding Server have the following body:

```json
{
  "errorCode": "ERROR_GENERIC",
  "message": "An error message"
}
```

## API Endpoints

Optional RESTful API services called by Onboarding Server:

<!-- begin api POST /client/approval -->
### Onboarding Approval Service

Service for onboarding process approval.

<!-- begin remove -->
<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/client/approval</code></td>
    </tr>
</table>
<!-- end -->

#### Request

```json
{
    "processId": "String",
    "userId": "String",
    "identityVerificationId": "String",
    "provider": "String",
    "result": Number,
    "biometryData": "String"
}
```

##### Request Params

| Attribute                | Type      | Description                                                                                                                            |
|:-------------------------|:----------|:---------------------------------------------------------------------------------------------------------------------------------------|
| `processId`              | `String`  | ID of an onboarding process.                                                                                                           |
| `userId`                 | `String`  | ID of a user stored on onboarding process.                                                                                             |
| `identityVerificationId` | `String`  | ID of the document verification subprocess.                                                                                            |
| `evaluationResult`       | `String`  | The evaluation outcome OK, NOK and WAIT. Process should either continue (OK), or fail/reset (NOK) or wait for asynchronous evaluation. |
| `provider`               | `String`  | Name of the configured external biometry provider. Currently "iProov".                                                                 |
| `result`                 | `Number` | Outcome of the verification check on scale 0-10.                                                                                       |
| `biometryData`           | `String`  | Photo/image identifier from the biometry session, encoded in base64.                                                                   |

#### Response 200

```json
{
  "result": "String",
  "resultReason": null
}
```

##### Response  Params

| Attribute      | Type     | Description                                                                                                                                      |
|:---------------|:---------|:-------------------------------------------------------------------------------------------------------------------------------------------------|
| `result`       | `String` | The approval outcome OK, NOK and WAIT. Process should either continue (OK), or fail/reset (NOK) or WAIT for asynchronous evaluation.             |
| `resultReason` | `String` | The reason is used when result is FAIL to disclose the reason of failed process (for example user started new document verification subprocess). |
<!-- end -->

<!-- begin api POST /user/lookup -->
### User Lookup Service

Service to identify the prospect and assign user identifier.

<!-- begin remove -->
<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/user/lookup</code></td>
    </tr>
</table>
<!-- end -->

#### Request

```json
{
    "processId": "String",
    "identification": Object # Dictionary/Hashmap
}
```

##### Request Params

| Attribute        | Type     | Description                                                                                                                                                                                                                                                                                                                                                              |
|:-----------------|:---------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `processId`      | `String` | ID of an onboarding process.                                                                                                                                                                                                                                                                                                                                             |
| `identification` | `Object` | The credentials passed from the mobile app. This attribute should be defined as dictionary/hashmap with [free-form object](https://swagger.io/docs/specification/v3_0/data-models/dictionaries/#free-form-objects) type. Keys in the dictionary can differ depending on use-case on specific instance. Example value `{"clientNumber": "String","birthDate": "String"}` |

#### Response 200

```json
{
  "userId": "String",
  "consent": "String",
  "consentRequired": Boolean # Deprecated
}
```

##### Response  Params

| Attribute                      | Type      | Description                                                                                                                            |
|:-------------------------------|:----------|:---------------------------------------------------------------------------------------------------------------------------------------|
| `userId`                       | `String`  | The bank assigned ID for the user being onboarded.                                                                                     |
| `consent`                      | `String`  | Tells if the user has to consent the onboarding (REQUIRED/NOT_REQUIRED) or if it's not evaluated by backend (NOT_EVALUATED or `null`). |
| `consentRequired (deprecated)` | `Boolean` | Tells if the user has to consent the onboarding.                                                                                       |
<!-- end -->

<!-- begin api POST /client/evaluation -->
### Client Evaluation Service

Service to evaluate data from the scanned documents. 

NOTE: Currently triggered only in positive result.

<!-- begin remove -->
<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/client/evaluation</code></td>
    </tr>
</table>
<!-- end -->

#### Request

```json
{
    "processId": "String",
    "userId": "String",
    "identityVerificationId": "String",
    "provider": "String",
    "investigationId": "String", # verificationId - upstream, investigationId - downstream
    "verificationResult": Object, # TODO
    "extractedData": Object # TODO
}
```

##### Request Params

| Attribute                | Type     | Description                                                                               |
|:-------------------------|:---------|:------------------------------------------------------------------------------------------|
| `processId`              | `String` | ID of an onboarding process.                                                              |
| `userId`                 | `String` | ID of a user stored on onboarding process.                                                |
| `identityVerificationId` | `String` | ID of the document verification subprocess.                                               |
| `provider`               | `String` | Name of the configured external document provider: ZenID or Microblink.                   |
| `investigationId`        | `String` | Optional ID of verification in the external document verifier. It is not used by default. |
| `verificationResult`     | `Object` | The data set containing the result of overall and partials verification checks.           |
| `extractedData`          | `Object` | The data set containing all mined data from the document.                                 |

#### Response 200

```json
{
  "result": "String",
  "resultReason": null
}
```

##### Response  Params

| Attribute      | Type     | Description                                                                                                                                      |
|:---------------|:---------|:-------------------------------------------------------------------------------------------------------------------------------------------------|
| `result`       | `String` | The approval outcome OK, NOK and WAIT. Process should either continue (OK), or fail/reset (NOK) or WAIT for asynchronous evaluation.             |
| `resultReason` | `String` | The reason is used when result is FAIL to disclose the reason of failed process (for example user started new document verification subprocess). |
<!-- end -->

<!-- begin api POST /otp/send -->
### OTP Delivery Service

Service to send generated OTPs.

<!-- begin remove -->
<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/otp/send</code></td>
    </tr>
</table>
<!-- end -->

#### Request

```json
{
    "processId": "String",
    "userId": "String",
    "language": "String",
    "otpCode": "String",
    "otpType": "String",
    "resend": Boolean
}
```

##### Request Params

| Attribute   | Type      | Description                                                           |
|:------------|:----------|:----------------------------------------------------------------------|
| `processId` | `String`  | ID of an onboarding process.                                          |
| `userId`    | `String`  | ID of a user stored on onboarding process.                            |
| `language`  | `String`  | The language used by the mobile app in ISO 639-1 format.              |
| `otpCode`   | `String`  | OTP generated by the system.                                          |
| `otpType`   | `String`  | Static value ACTIVATION.                                              |
| `resend`    | `Boolean` | Indication if the code is send for first time or resend is requested. |

#### Response 200

```json
{
  "otpSent": Boolean
}
```

##### Response  Params

| Attribute | Type      | Description                                   |
|:----------|:----------|:----------------------------------------------|
| `otpSent` | `Boolean` | Boolean value indicating if the OTP was sent. |
<!-- end -->

<!-- begin api POST /consent/storage -->
### Consent Storage Service

Service providing the storage of provided consents.

<!-- begin remove -->
<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/consent/storage</code></td>
    </tr>
</table>
<!-- end -->

#### Request

```json
{
    "processId": "String",
    "userId": "String",
    "consentType": "String",
    "approved": Boolean
}
```

##### Request Params

| Attribute     | Type      | Description                                     |
|:--------------|:----------|:------------------------------------------------|
| `processId`   | `String`  | ID of an onboarding process.                    |
| `userId`      | `String`  | ID of a user stored on onboarding process.      |
| `consentType` | `String`  | Type of the consent configured for the process. |
| `approved`    | `Boolean` | User approval or rejection.                     |

#### Response 204

```json
 
```
<!-- end -->

<!-- begin api POST /consent/text -->
### Consent Text Service

Service providing the text of the consent.

<!-- begin remove -->
<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/consent/text</code></td>
    </tr>
</table>
<!-- end -->

#### Request

```json
{
    "processId": "String",
    "userId": "String",
    "consentType": "String",
    "language": "String"
}
```

##### Request Params

| Attribute     | Type     | Description                                              |
|:--------------|:---------|:---------------------------------------------------------|
| `processId`   | `String` | ID of an onboarding process.                             |
| `userId`      | `String` | ID of a user stored on onboarding process.               |
| `consentType` | `String` | Type of the consent configured for the process.          |
| `language`    | `String` | The language used by the mobile app in ISO 639-1 format. |

#### Response 200

```json
{
  "consentText": "String"
}
```

##### Response  Params

| Attribute     | Type     | Description                                            |
|:--------------|:---------|:-------------------------------------------------------|
| `consentText` | `String` | Consent text in HTML format in the requested language. |
<!-- end -->