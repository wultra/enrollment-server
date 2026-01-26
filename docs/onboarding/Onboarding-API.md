# Onboarding API
<!-- template api -->

Onboarding Server provides a RESTful API that allows to control specific parts of the onboarding process. Usage of the API is optional and depends on the system configuration.

<!-- begin remove -->
- `POST` [/api/private/client/evaluate](#anchor) - Client Evaluation
- `POST` [/api/private/client/approve](#anchor) - Onboarding Approval
<!-- end -->

## Error Handling

Onboarding Server uses following format for error response body, accompanied by an appropriate HTTP status code. Besides the HTTP error codes that application server may return regardless of server application (such as 404 when resource is not found or 503 when server is down), following ERROR codes may be returned:

| Error Code         | HTTP Code | Description                                                |
|:-------------------|:----------|:-----------------------------------------------------------|
| ERROR_GENERIC      | 400       | Issue with a request format or issue of the business logic |
| ERROR_UNAUTHORIZED | 401       | Unauthorized request                                       |

All error responses that are produced by the Onboarding Server have the following body:

```json
{
  "status": "ERROR",
  "responseObject": {
    "code": "ERROR_GENERIC",
    "message": "An error message"
  }
}
```

##  API Endpoints

<!-- begin api POST /api/private/client/evaluate -->
###  Client Evaluation

If configured, the system waits for the event from an external system to continue with "client evaluation" phase. Use the endpoint bellow to proceed.

<!-- begin remove -->
<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/private/client/evaluate</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
    - `Content-Type: application/json`
    - `X-PowerAuth-Authorization: ...`

```json
{
  "processId": "String",
  "userId": "String",
  "identityVerificationId": "String",
  "evaluationResult": "String"
}
```

##### Request Params

| Attribute                | Type     | Description                                                                                 |
|:-------------------------|:---------|:--------------------------------------------------------------------------------------------|
| `processId`              | `String` | ID of an onboarding process.                                                                |
| `userId`                 | `String` | ID of a user stored on onboarding process.                                                  |
| `identityVerificationId` | `String` | ID of the identity verification subprocess.                                                 |
| `evaluationResult`       | `String` | The evaluation outcome OK or NOK. Process should either continue (OK), or fail/reset (NOK). |

#### Response 200

```json
{
  "result": "String",
  "resultReason": null
}
```

##### Response  Params

| Attribute      | Type     | Description                                                                                                                                     |
|:---------------|:---------|:------------------------------------------------------------------------------------------------------------------------------------------------|
| `result`       | `String` | The transition outcome OK, NOK. Depends on the transition to the next phase was successful.                                                     |
| `resultReason` | `String` | The reason is used when result is NOK to disclose the reason of failed process (for example user started new identity verification subprocess). |
<!-- end -->

<!-- begin api POST /api/private/client/approve -->
### Onboarding Approval

If configured, the system waits for the event from an external system to continue with "onboarding approval" phase. Use the endpoint below to proceed.

<!-- begin remove -->
<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/private/client/approve</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
    - `Content-Type: application/json`
    - `X-PowerAuth-Authorization: ...`

```json
{
  "processId": "String",
  "userId": "String",
  "identityVerificationId": "String",
  "approvalResult": "String"
}
```

##### Request Params

| Attribute                | Type     | Description                                                                               |
|:-------------------------|:---------|:------------------------------------------------------------------------------------------|
| `processId`              | `String` | ID of an onboarding process.                                                              |
| `userId`                 | `String` | ID of a user stored on onboarding process.                                                |
| `identityVerificationId` | `String` | ID of the identity verification subprocess.                                               |
| `approvalResult`         | `String` | The approval outcome OK or NOK. Process should either continue (OK), or fail/reset (NOK). |

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
| `result`       | `String` | The transition outcome OK, NOK. Depends on the transition to the next phase was successful.                                                     |
| `resultReason` | `String` | The reason is used when result is NOK to disclose the reason of failed process (for example user started new identity verification subprocess). |
<!-- end -->