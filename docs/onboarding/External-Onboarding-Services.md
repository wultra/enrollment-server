# External Onboarding Services
<!-- template api -->

Onboarding Server can call services exposed by the entity to extend the KYC process. These services need to be implemented by the client, so the documentation below describes API requirements, not existing services.

<!-- begin remove -->
- `POST` [/client/approve](#anchor) - Onboarding Approval Service
- `POST` [/user/lookup](#anchor) - User Lookup Service
- `POST` [/client/evaluate](#anchor) - Client Evaluation Service
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
https://my.great.api/api/v1/client/approve
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

<!-- begin api POST /client/approve -->
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
        <td><code>/client/approve</code></td>
    </tr>
</table>
<!-- end -->

#### Request

```json
{
    "processId": "String",
    "processType": "String",
    "userId": "String",
    "identityVerificationId": "String",
    "provider": "String",
    "status": "String",
    "score": Number,
    "presenceCheckResult": {
      "frame": "String"
    }
}
```

##### Request Params

| Attribute                   | Type     | Description                                                               |
|:----------------------------|:---------|:--------------------------------------------------------------------------|
| `processId`                 | `String` | ID of an onboarding process.                                              |
| `processType`               | `String` | Type of the onboarding process.                                           |
| `userId`                    | `String` | ID of a user stored on onboarding process.                                |
| `identityVerificationId`    | `String` | ID of the identity verification subprocess.                               |
| `provider`                  | `String` | Name of the configured external biometry provider. For example, `iProov`. |
| `status`                    | `String` | Status of the identity verification process, `SUCCESS` or `FAILURE`.      |
| `score`                     | `Number` | Outcome confidence of the verification check on scale 0-10.               |
| `presenceCheckResult.frame` | `String` | Photo/image from the biometry session, encoded in base64.                 |

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
| `resultReason` | `String` | The reason is used when result is NOK to disclose the reason of failed process (for example user started new identity verification subprocess).  |
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
    "processType": "String",
    "identification": Object # Dictionary/Hashmap
}
```

##### Request Params

| Attribute        | Type     | Description                                                                                                                                                                                                                                                                                                                                                             |
|:-----------------|:---------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `processId`      | `String` | ID of an onboarding process.                                                                                                                                                                                                                                                                                                                                            |
| `processType`    | `String` | Type of the onboarding process.                                                                                                                                                                                                                                                                                                                                         |
| `identification` | `Object` | The credentials passed from the mobile app. This attribute should be defined as dictionary/hashmap with [free-form object](https://swagger.io/docs/specification/v3_0/data-models/dictionaries/#free-form-objects) type. Keys in the dictionary can differ depending on use-case on specific instance. Example value `{"clientNumber": "String","birthDate": "String"}` |

#### Response 200

```json
{
  "userId": "String",
  "consentRequired": true
}
```

##### Response Params

| Attribute         | Type      | Description                                                                                                                                                                                       |
|:------------------|:----------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `userId`          | `String`  | The bank assigned ID for the user being onboarded.                                                                                                                                                |
| `consentRequired` | `Boolean` | Tells if the user has to consent the onboarding, `null` is evaluated as `true`. Taken into account, only if [the procces is configured](Configuration-Onboarding-Process.md) to require consents. |
<!-- end -->

<!-- begin api POST /client/evaluate -->
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
        <td><code>/client/evaluate</code></td>
    </tr>
</table>
<!-- end -->

#### Request

```json
{
    "processId": "String",
    "processType": "String",
    "userId": "String",
    "identityVerificationId": "String",
    "provider": "String",
    "status": "String",
    "documentCheckResult": {
        "person": {
            "surname": "String",
            "givenNames": "String",
            "dateOfBirth": "String"
        },
        "documents": [
            {
                "type": "String",
                "country": "String",
                "status": "String",
                "score": Number,
                "data": {
                    "surname": "String",
                    "givenNames": "String",
                    "dateOfBirth": "String",
                    "placeOfBirth": "String",
                    "sex": "String",
                    "nationality": "String",
                    "personalNumber": "String",
                    "documentNumber": "String",
                    "dateOfIssue": "String",
                    "dateOfExpiry": "String",
                    "authority": "String"
                },
                "images": [
                    {
                        "type": "String",
                        "data": "String"
                    }
                ],
                "rawData": Object
            }
        ]
    }
}
```

##### Request Params

| Attribute                                   | Type     | Description                                                                                                                                                                                  |
|:--------------------------------------------|:---------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `processId`                                 | `String` | ID of an onboarding process.                                                                                                                                                                 |
| `processType`                               | `String` | Type of the onboarding process.                                                                                                                                                              |
| `userId`                                    | `String` | ID of a user stored on onboarding process.                                                                                                                                                   |
| `identityVerificationId`                    | `String` | ID of the identity verification subprocess.                                                                                                                                                  |
| `provider`                                  | `String` | Name of the configured external document provider: ZenID or Microblink.                                                                                                                      |
| `status`                                    | `String` | Status of the identity verification process, `SUCCESS` or `FAILURE`.                                                                                                                         |
| `documentCheckResult.person`                | `Object` | Selected normalized data extracted from the documents. Data are cross-checked between the documents. Date values are in ISO 8601 format `YYYY-MM-DD` (e.g. `2010-01-21`).                    |
| `documentCheckResult.documents`             | `Array`  | Array of documents containing all mined data.                                                                                                                                                |
| `documentCheckResult.documents.type`        | `String` | Document type, eg. `ID_CARD`, `PASSPORT`, `DRIVING_LICENSE`.                                                                                                                                 |
| `documentCheckResult.documents.country`     | `String` | Document country in ISO Alpha 3 format, eg. `CZE`.                                                                                                                                           |
| `documentCheckResult.documents.status`      | `String` | Status of the identity verification process for specific document, `SUCCESS` or `FAILURE`.                                                                                                   |
| `documentCheckResult.documents.score`       | `Number` | Outcome confidence of the verification check on scale 0-10.                                                                                                                                  |
| `documentCheckResult.documents.data`        | `Object` | Selected normalized data extracted from the document. See `documentCheckResult.documents.rawData` for complete results. Date values are in ISO 8601 format `YYYY-MM-DD` (e.g. `2010-01-21`). |
| `documentCheckResult.documents.images`      | `Array`  | Array of images extracted from the document.                                                                                                                                                 |
| `documentCheckResult.documents.images.type` | `String` | Image type, e.g. `FACE`.                                                                                                                                                                     |
| `documentCheckResult.documents.images.data` | `String` | JPEG binary data encoded using `base64`.                                                                                                                                                     |
| `documentCheckResult.documents.rawData`     | `Object` | Complete response from verification provider with verification status, performed checks and extracted data.                                                                                  |

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
| `resultReason` | `String` | The reason is used when result is NOK to disclose the reason of failed process (for example user started new identity verification subprocess).  |
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
    "processType": "String",
    "userId": "String",
    "language": "String",
    "otpCode": "String",
    "otpType": "String",
    "resend": Boolean
}
```

##### Request Params

| Attribute     | Type      | Description                                                           |
|:--------------|:----------|:----------------------------------------------------------------------|
| `processId`   | `String`  | ID of an onboarding process.                                          |
| `processType` | `String`  | Type of the onboarding process.                                       |
| `userId`      | `String`  | ID of a user stored on onboarding process.                            |
| `language`    | `String`  | The language used by the mobile app in ISO 639-1 format.              |
| `otpCode`     | `String`  | OTP generated by the system.                                          |
| `otpType`     | `String`  | Static value ACTIVATION.                                              |
| `resend`      | `Boolean` | Indication if the code is send for first time or resend is requested. |

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
    "processType": "String",
    "userId": "String",
    "consentType": "String",
    "approved": Boolean
}
```

##### Request Params

| Attribute     | Type      | Description                                     |
|:--------------|:----------|:------------------------------------------------|
| `processId`   | `String`  | ID of an onboarding process.                    |
| `processType` | `String`  | Type of the onboarding process.                 |
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
    "processType": "String",
    "userId": "String",
    "consentType": "String",
    "language": "String"
}
```

##### Request Params

| Attribute     | Type     | Description                                              |
|:--------------|:---------|:---------------------------------------------------------|
| `processId`   | `String` | ID of an onboarding process.                             |
| `processType` | `String` | Type of the onboarding process.                          |
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