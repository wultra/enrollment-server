# Onboarding Process Configuration

This document describes the JSON configuration stored in the `config` column of the `es_onboarding_process_configuration` table.
Each configuration is associated with a `process_type` (e.g. `onboarding` or `reactivation`), which serves as an identifier for the specific onboarding flow.


## Root Object

| Field                        | Type      | Default    | Description                                                                                                       |
|------------------------------|-----------|------------|-------------------------------------------------------------------------------------------------------------------|
| `enabled`                    | `boolean` | `false`    | Whether the process type is enabled.                                                                              |
| `otpForIdentification`       | `boolean` | `false`    | Whether the OTP is required for the initial identification of the user.                                           |
| `otpForIdentityVerification` | `boolean` | `false`    | Whether the OTP is required for identity verification - request OTP for the next process step.                    |
| `useTemporaryActivation`     | `boolean` | `false`    | Whether the onboarding process should use two activations, and exchange the temporary one for the permanent one.  |
| `documents`                  | `object`  | `(empty)`  | Configuration of required documents. See [Documents](#documents) below.                                           |
| `activationType`             | `string`  | `IDENTITY` | Whether the activation is initialized by the onboarding server (`CODE`) or by the SDK (`IDENTITY`).               |
| `approvalEnabled`            | `boolean` | `false`    | Whether to call bank systems to approve the client.                                                               |
| `clientEvaluationEnabled`    | `boolean` | `true`     | Whether to call bank systems to evaluate the client.                                                              |
| `verifyPresenceWithOtp`      | `boolean` | `true`     | Whether to verify the presence with OTP. If `true`, hide the result of presence check until valid OTP is entered. |
| `consentRequired`            | `boolean` | `false`    | Specifies whether user consent must be accepted.                                                                  |


## Documents

| Field                         | Type     | Default | Description                                                 |
|-------------------------------|----------|---------|-------------------------------------------------------------|
| `totalRequiredDocumentsCount` | `number` | `0`     | Number of required documents to submit.                     |
| `groups`                      | `array`  | `[]`    | Set of document groups. See [Group](#document-group) below. |


## Document Group

| Field                    | Type     | Default | Description                                                           |
|--------------------------|----------|---------|-----------------------------------------------------------------------|
| `requiredDocumentsCount` | `number` | `0`     | Number of required documents from this group.                         |
| `items`                  | `array`  | `[]`    | Set of document configurations. See [Document](#document-item) below. |


## Document Item

| Field       | Type     | Default | Description                                              |
|-------------|----------|---------|----------------------------------------------------------|
| `type`      | `string` |         | Document type: `ID_CARD`, `PASSPORT`, `DRIVING_LICENSE`. |
| `sideCount` | `number` | `1`     | Info if the document contains one or two sides (1 or 2). |
| `country`   | `string` |         | Document country as an ISO 3166-1 alpha-3 code.          |


## Example

```json
{
  "enabled": true,
  "activationType": "CODE",
  "otpForIdentification": true,
  "otpForIdentityVerification": true,
  "useTemporaryActivation": true,
  "approvalEnabled": true,
  "verifyPresenceWithOtp": false,
  "consentRequired": true,
  "documents": {
    "totalRequiredDocumentsCount": 2,
    "groups": [
      {
        "requiredDocumentsCount": 1,
        "items": [
          {
            "type": "ID_CARD",
            "sideCount": 2,
            "country": "CZE"
          },
          {
            "type": "PASSPORT",
            "sideCount": 1
          }
        ]
      },
      {
        "requiredDocumentsCount": 0,
        "items": [
          {
            "type": "DRIVING_LICENSE",
            "sideCount": 1
          }
        ]
      }
    ]
  }
}
```
