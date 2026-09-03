# Events

This page describes the event types produced by the Enrollment Server Onboarding.

List of event types:
- [DOCUMENT_VERIFICATION_FINISHED](#event-data-for-document_verification_finished)
- [FINAL_DOCUMENT_VERIFICATION_FINISHED](#event-data-for-final_document_verification_finished)
- [PRESENCE_CHECK_FINISHED](#event-data-for-presence_check_finished)
- [PROCESS_FINISHED](#event-data-for-process_finished)


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
| `rejectReason`               | String | Reject reason when `status` is `REJECTED`. The reason is returned by the verification provider, or `documentVerificationRejected` when the provider omits it. Otherwise itis `null`. Value details are described under the table.                                                                                                                                         |
| `errorDetail`                | String | Error detail in case `status` is `FAILED`. Otherwise is `null`.                                                                                                                                                                                         |
| `provider`                   | String | Name of the configured external biometry provider. For example, `Microblink`.                                                                                                                                                                           |
| `score`                      | Number | Outcome confidence of the verification check on scale 0-10.                                                                                                                                                                                             |
| `documentVerificationResult` | Object | Contains some details about the document and extracted data. Object is present only if `status` is `ACCEPTED` or `REJECTED`. Otherwise it is `null`. Complete response from verification provider can be found in `documentVerificationResult.rawData`. |

**Reject Reason Format - Microblink**

The value of the field `rejectReason` for the Microblink provider has the following format:
```
Rejected by provider [{code} {messages}, {code} {messages}...] # there can be more messages
```

Example response:
```
Rejected by provider [W001 The image quality is below the configured verification threshold. Ensure the image is sharp, well-lit, and free of blur or glare., I002 Cropping did not block verification because cropAffectsVerdict was false.]
```

All `code` and `message` values are described in the section [Reject Reasons for Microblink](#reject-reasons-for-microblink)


### Event data for FINAL_DOCUMENT_VERIFICATION_FINISHED

This contains the overall document check result for all documents combined.

Bear in mind that even if the individual documents were verified by the verification provider, the overall document check result could still be negative due to the additional checks performed.

Additional checks:
- document type - checks whether the required document type matches the uploaded document type
- document crosscheck - the first name, surname and date of birth, in case the user has uploaded multiple documents

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

| Attribute                | Type   | Description                                                                                                        |
|:-------------------------|:-------|:-------------------------------------------------------------------------------------------------------------------|
| `documentVerificationId` | String | Document Verification ID.                                                                                          |
| `status`                 | String | Status of the verification. Supported values are `ACCEPTED`, `REJECTED` and `FAILED`.                              |
| `rejectReason`           | String | Reject reason in case of `status` is `REJECTED`. Otherwise is `null`. Value details are described under the table. |
| `errorDetail`            | String | Error detail in case of `status` is `FAILED`. Otherwise is `null`.                                                 |
| `provider`               | String | Name of the configured external biometry provider. For example, `Microblink`.                                      |
| `documentIds`            | Array  | Array of verified Document IDs.                                                                                    |

**Reject Reason Format - Microblink**

The value of the field `rejectReason` for the Microblink provider has one of the following formats:
```
Rejected by provider [{code} {messages}, {code} {messages}...]
or
Extracted document type {extractedType} does not match claimed type {claimedType}
or
Document data crosscheck failed for fields: [{field}, {field}...]
```

Example responses:
```
Rejected by provider [W001 The image quality is below the configured verification threshold. Ensure the image is sharp, well-lit, and free of blur or glare., I002 Cropping did not block verification because cropAffectsVerdict was false.]
or
Extracted document type DRIVING_LICENSE does not match claimed type ID_CARD
or
Document data crosscheck failed for fields: [firstName, lastName, dateOfBirth]
```

- All `code` and `message` values are described in the section [Reject Reasons for Microblink](#Reject-reasons-for-microblink)
- All `extractedType` and `claimedType` values are based on currently supported document types `ID_CARD`, `PASSPORT` and `DRIVING_LICENSE`
- All `field` values are `firstName`, `lastName` and `dateOfBirth`


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

| Attribute                   | Type   | Description                                                                                                                                                                                               |
|:----------------------------|:-------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `status`                    | String | Status of the verification. Supported values are `ACCEPTED`, `REJECTED` and `FAILED`.                                                                                                                     |
| `rejectReason`              | String | Reject reason in case of `status` is `REJECTED`. Otherwise is `null`. The value is taken from the provider's response, messages are described in [Reject Reasons for iProov](#reject-reasons-for-iproov). |
| `errorDetail`               | String | Error detail in case of `status` is `FAILED`. Otherwise is `null`.                                                                                                                                        |
| `provider`                  | String | Name of the configured external biometry/presence-check provider. For example, `iProov`.                                                                                                                  |
| `score`                     | Number | Outcome confidence of the verification check on scale 0-10.                                                                                                                                               |
| `presenceCheckResult.frame` | String | Photo/image from the biometry session, encoded in base64.                                                                                                                                                 |


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

## Verification Providers Reject Reasons

### Reject Reasons for Microblink

The table below shows all possible values for the `code` and `message` attributes in the `rejectReason` field.

| Code | Message                                                                                                                                                                  |
|:-----|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| W001 | The image quality is below the configured verification threshold. Ensure the image is sharp, well-lit, and free of blur or glare.                                        |
| W002 | Only one side of a multi-sided document was received. Supply the missing side for complete verification and higher confidence.                                           |
| W003 | The barcode could not be read. Ensure it is fully visible, in focus, and free of blur or glare. If available, supply a separate barcode image.                           |
| W004 | Screen-presence, photocopy, and generative-AI checks were not performed because verificationContext is InPerson.                                                         |
| W005 | This document version is not accepted for verification.                                                                                                                  |
| W006 | The document is expired and rejectExpiredDocuments is enabled, resulting in a Reject verdict.                                                                            |
| W007 | Verification completed using only the first side because the second side could not be used. Supply a clear image of the matching second side for a more complete result. |
| W008 | The document is cropped or too close to the image edges, preventing a conclusive verification result. Supply an uncropped image with all document edges visible.         |
| W009 | The supplied image appears to be the wrong document side while the first side is still required. Supply a clear image of the first side to continue verification.        |
| I001 | Despite the image-quality warning, verification returned an Accept verdict under the configured image-quality retry policy.                                              |
| I002 | Cropping did not block verification because cropAffectsVerdict was false.                                                                                                |
| I003 | The document is expired, but rejectExpiredDocuments is disabled, so expiration did not affect the verification verdict.                                                  |


### Reject Reasons for iProov

The table below shows all possible values in the `rejectReason` field.

| Message                                                        |
|:---------------------------------------------------------------|
| Please keep still                                              |
| Strong light source detected behind you. Try turning around    |
| Your environment appears too dark. Try turning the lights on   |
| Too much light detected on your face                           |
| Ambient light too strong or screen brightness too low          |
| Please do not talk while iProoving                             |
| Sorry, ambiguous outcome                                       |
| Sorry, your session has timed out                              |
| Sorry, your device is not supported at the moment              |
