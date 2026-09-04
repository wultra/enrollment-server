# Events

This page describes the event types produced by the Enrollment Server Onboarding.

List of event types:
- [DOCUMENT_VERIFICATION_FINISHED](#Event-data-for-DOCUMENT_VERIFICATION_FINISHED)
- [FINAL_DOCUMENT_VERIFICATION_FINISHED](#Event-data-for-FINAL_DOCUMENT_VERIFICATION_FINISHED)
- [PRESENCE_CHECK_FINISHED](#Event-data-for-PRESENCE_CHECK_FINISHED)
- [PROCESS_FINISHED](#Event-data-for-PROCESS_FINISHED)


## Structure

This is the generic part of the event, which is common to all event types.

```json
{
    "id": "8f0d649e-a688-4ead-8581-fc2b67e88be4",
    "timestamp": "2026-04-23T14:30:00Z",
    "type": "PROCESS_FINISHED",
    "userId": "40405309-6406-4d6b-b4ef-642e52ac44f4",
    "externalUserId": "629199e8-aa0d-4fc0-911c-089d53e0f608",
    "processId": "8b2dfae4-d955-4d8f-a95b-2d9c5a4b0e26",
    "processType": "onboarding",
    "identityVerificationId": "d3827099-3b6c-4df9-887d-4eac402fc4f9",
    "eventData": {}
}
```

| Attribute                | Type   | Description                                                                                                                                                            |
|:-------------------------|:-------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`                     | String | Event ID.                                                                                                                                                              |
| `timestamp`              | String | Timestamp, when the action happened.                                                                                                                                   |
| `type`                   | String | Event type. Currently supported types are `DOCUMENT_VERIFICATION_FINISHED`, `FINAL_DOCUMENT_VERIFICATION_FINISHED`, `PRESENCE_CHECK_FINISHED`, and `PROCESS_FINISHED`. |
| `userId`                 | String | User ID.                                                                                                                                                               |
| `externalUserId`         | String | External User ID. A fixed user ID used for the presence check provider. It is set to `null` at the start of the process and created during the presence check phase.   |
| `processId`              | String | Process ID.                                                                                                                                                            |
| `processType`            | String | Name of the process, e.g. `onboarding`.                                                                                                                                |
| `identityVerificationId` | String | Identity Verification ID. Can be `null` if the event is not related to the identity verification stage.                                                                |
| `eventData`              | Object | Object with structure different for each `type`.                                                                                                                       |


## Event data for different event types
 
Different event types have different structures in `eventData`.


### Event data for DOCUMENT_VERIFICATION_FINISHED

This contains the results from the verification provider. Each document is sent separately.

```json
{
    "documentVerification": {
        "documentVerificationId": "String",
        "documentId": "String",
        "status": "String",
        "rejectReason": null,
        "errorDetail": null,
        "provider": "String",
        "score": Number,
        "documentVerificationResult": {
            "type": "String",
            "country": "String",
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
    }
}
```

| Attribute                    | Type   | Description                                                                                                                                                                                                                                             |
|:-----------------------------|:-------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `documentVerificationId`     | String | Document Verification ID.                                                                                                                                                                                                                               |
| `documentId`                 | String | Document ID.                                                                                                                                                                                                                                            |
| `status`                     | String | Status of the verification. Supported values are `ACCEPTED`, `REJECTED` and `FAILED`.                                                                                                                                                                   |
| `rejectReason`               | String | Reject reason when `status` is `REJECTED`. The reason is returned by the document verification provider, or `Other` when the provider omits it. Otherwise it is `null`.                                                                                 |
| `errorDetail`                | String | Error detail in case `status` is `FAILED`. Otherwise is `null`.                                                                                                                                                                                         |
| `provider`                   | String | Name of the configured external biometry provider. For example, `Microblink`.                                                                                                                                                                           |
| `score`                      | Number | Outcome confidence of the verification check on scale 0-10.                                                                                                                                                                                             |
| `documentVerificationResult` | Object | Contains some details about the document and extracted data. Object is present only if `status` is `ACCEPTED` or `REJECTED`. Otherwise it is `null`. Complete response from verification provider can be found in `documentVerificationResult.rawData`. |


### Event data for FINAL_DOCUMENT_VERIFICATION_FINISHED

This contains the overall document check result for all documents combined.

Bear in mind that even if the individual documents were verified by the verification provider, the overall document check result could still be negative due to the additional checks performed.

Additional checks:
- document crosscheck
- document type
- document country

```json
{
    "finalDocumentVerification": {
        "documentVerificationId": "String",
        "status": "String",
        "rejectReason": null,
        "errorDetail": null,
        "provider": "String",
        "documentIds": ["String","String"]
    }
}
```

| Attribute                | Type   | Description                                                                           |
|:-------------------------|:-------|:--------------------------------------------------------------------------------------|
| `documentVerificationId` | String | Document Verification ID.                                                             |
| `status`                 | String | Status of the verification. Supported values are `ACCEPTED`, `REJECTED` and `FAILED`. |
| `rejectReason`           | String | Reject reason in case of `status` is `REJECTED`. Otherwise is `null`.                 |
| `errorDetail`            | String | Error detail in case of `status` is `FAILED`. Otherwise is `null`.                    |
| `provider`               | String | Name of the configured external biometry provider. For example, `Microblink`.         |
| `documentIds`            | Array  | Array of verified Document IDs.                                                       |


### Event data for PRESENCE_CHECK_FINISHED

This contains the results of the verification provider.

```json
{
    "presenceCheck": {
        "status": "ACCEPTED",
        "rejectReason": null,
        "errorDetail": null,
        "provider": "iProov",
        "score": Number,
        "presenceCheckResult": {
          "frame": "String"
        }
    }
}
```

| Attribute                   | Type   | Description                                                                           |
|:----------------------------|:-------|:--------------------------------------------------------------------------------------|
| `status`                    | String | Status of the verification. Supported values are `ACCEPTED`, `REJECTED` and `FAILED`. |
| `rejectReason`              | String | Reject reason in case of `status` is `REJECTED`. Otherwise is `null`.                 |
| `errorDetail`               | String | Error detail in case of `status` is `FAILED`. Otherwise is `null`.                    |
| `provider`                  | String | Name of the configured external biometry/presence-check provider. For example, `iProov`. |
| `score`                     | Number | Outcome confidence of the verification check on scale 0-10.                           |
| `presenceCheckResult.frame` | String | Photo/image from the biometry session, encoded in base64.                             |


### Event data for PROCESS_FINISHED

This contains final process data.

```json
{
    "process": {
        "status": "FINISHED",
        "errorDetail": null,
        "deviceData": {
            "locale": "EN",
            "ipAddress": null,
            "httpUserAgent": null,
            "fdsData": Object
        }
    }
}
```

| Attribute                  | Type   | Description                                                                                                          |
|:---------------------------|:-------|:---------------------------------------------------------------------------------------------------------------------|
| `status`                   | String | Status of the process. Supported values are `FINISHED` and `FAILED`.                                                 |
| `errorDetail`              | String | Error detail in case of `status` is `FAILED`. Otherwise is `null`.                                                   |
| `deviceData.locale`        | String | Client locale recorded during process initialization.                                                                |
| `deviceData.ipAddress`     | String | Client IP address. Keep in mind that IP Address can change during Onboarding process.                                |
| `deviceData.httpUserAgent` | String | Client User-Agent recorded during process initialization.                                                            |
| `deviceData.fdsData`       | Object | Optional FDS data sent from the mobile device during Onboarding initialization using `/api/onboarding/start` method. |
