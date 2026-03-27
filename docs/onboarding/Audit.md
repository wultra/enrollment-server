# Audit

This feature provides audit logging for the onboarding server in the database.
For more detailed developer documentation see [auditing library documentation](https://github.com/wultra/java-core?tab=readme-ov-file#wultra-auditing-library).

<!-- begin box warning -->
The audit log should be used to investigate specific onboarding processes. It should not be used for reporting, as it is not optimized for this purpose
and would negatively impact performance.
<!-- end -->

A value in the `audit_log.audit_type` column is used to categorize the audit log entry according to the operation scope.
The following values are used:

| Value                          | Description                                                                                                                                                                                                                            |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `process`                      | High-level lifecycle events for onboarding processing, including start, resume, completion, cleanup, expiry, and limit handling. Use this type to reconstruct the overall timeline and final outcome of a case.                        |
| `otp`                          | OTP control events such as send/resend, verification success or failure, expiration, cancellation, and max-attempt handling. This type is used to audit MFA step integrity and detect abuse patterns.                                  |
| `identityVerification`         | State and status transitions of identity verification phases (for example document upload, presence check, client evaluation, and completion). This is the primary type for tracking decision flow across verification stages.         |
| `activation`                   | Events related to PowerAuth activation lifecycle during onboarding, including creation, replacement, and cleanup of activation identifiers. Use it to validate activation consistency and rollback behavior.                           |
| `documentVerification`         | Per-document processing and decision events (pending, accepted, rejected, failed), including final document-level outcomes. This type enables forensic traceability for each submitted document artifact.                              |
| `presenceCheckProvider`        | Events from the liveness/presence provider, including initialization, execution, result retrieval, and cleanup. Use this type to review biometric verification path and provider-side failures.                                        |
| `documentVerificationProvider` | Events from the document verification provider, including submissions, evaluation retrieval, cross-verification, SDK initialization, and cleanup. This type is useful for proving provider interaction sequence and response handling. |
| `onboardingProvider`           | Calls to external [onboarding providers](./External-Onboarding-Services.md) for business services such as lookup, consent, OTP delivery, and client evaluation/approval.                                                               |


## Common parameters

These parameters are used across event types:

| Parameter                    | Description                                                                                       |
|------------------------------|---------------------------------------------------------------------------------------------------|
| `processId`                  | ID of the onboarding process                                                                      |
| `activationId`               | PowerAuth activation ID bind with the onboarding process                                          |
| `userId`                     | ID of user passing the onboarding process                                                         |
| `documentVerificationStatus` | Status of document verification. Possible values: `IN_PROGRESS`, `ACCEPTED`, `REJECTED`, `FAILED` |


## Event type `process`


### Message `Process started for user: {userId}`

The onboarding process was created and started. The endpoint `POST api/onboarding/start` was called and existing process was not found for the provided user identification data.


### Message `Process resumed for user: {userId}`

The onboarding process was resumed. The endpoint `POST api/onboarding/start` was called and existing process was found for the provided user identification data.


### Message `Process finished for user: {userId}`

The onboarding process was successfully finished.


### Message `Process failed: {errorDetail}, for user: {userId}`

The onboarding process failed. See detail in the `errorDetail` parameter.


### Message `Expired process for user: {userId}, {errorDetail}`

The onboarding process was not completed in a given time, and the process is cleaned up by a scheduled job.

Event-specific parameters:



| Parameter     | Description                                                                                                                                                                                                                                                |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `errorDetail` | Possible values: <br/> `expiredProcessOnboarding` - the onboarding process itself expired <br/> `expiredProcessActivation` - the linked PowerAuth activation expired <br/> `expiredProcessIdentityVerification` - the linked identity verification expired |


### Message `Max failed attempts reached for identity verification for user: {userId}`

Too many identity verifications were attempted.


### Message `Maximum number of processes per day reached for user: {userId}`

Maximum number of processes per day reached.


### Message `Presence check max failed attempts reached for user: {userId}`

Too many attempts were made to verify the presence check.


### Message `Process cleaned up for user: {userId}`

The onboarding process was cleaned up manually by calling the endpoint `POST /api/onboarding/cleanup`.


## Event type `otp`


### Event type-specific parameters

| Parameter | Description                                                     |
|-----------|-----------------------------------------------------------------|
| `otpType` | Type of OTP. Possible values: `ACTIVATION`, `USER_VERIFICATION` |
| `otpId`   | ID of the OTP used in the onboarding process step               |


### Message `OTP failed for user: {userId}`

Previously verified user verification OTP was invalidated and marked as failed. This happens when the OTP step succeeds,
but the combined SCA evaluation fails because the earlier presence check result was not successful.


### Message `Expired OTP for user: {userId}`

The OTP was not used in a given time, and the process is cleaned up by a scheduled job.


### Message `OTP expired for user: {userId}`

An OTP code was submitted after its expiration time and could no longer be verified.


### Message `OTP {otpType} verified for user: {userId}`

The user submitted valid OTP code.


### Message `OTP {otpType} verification failed for user: {userId}`

The user submitted invalid OTP code.


### Message `OTP max attempts reached for user: {userId}`

Too many OTP verification attempts were made.


### Message `OTP failed because of failed process for user: {userId}`

The onboarding process is failed and the OTP verification is not possible.


### Message `Resending OTP for user: {userId}`

The user requested to resend the OTP code.


### Message `OTP canceled for process ID: {processId}, user ID: {userId}, otp type: {otpType}`

Invalidate the OTP code. This is done by calling the endpoint `POST /api/onboarding/cleanup`.


## Event type `identityVerification`


### Event type-specific parameters

| Parameter                 | Description                                                        |
|---------------------------|--------------------------------------------------------------------|
| `clientApprovalResult`    | Result of client approval. Possible values: `OK`, `NOK`, `WAIT`    |
| `clientEvaluationResult`  | Result of client evaluation. Possible values: `OK`, `NOK`, `WAIT`  |


### Message `Acknowledged onboarding approval result: {clientApprovalResult}`

Client approval result was received on the endpoint `POST /api/private/client/approve`. The endpoint is called when approval is handled asynchronously.


### Message `Acknowledged evaluation approval result: {clientEvaluationResult}`

Client evaluation result was received on the endpoint `POST /api/private/client/evaluate`. The endpoint is called when evaluation is handled asynchronously.


### Message `Expired identity verification for user: {userId}, {errorDetail}`

Identity verification wasn't completed in a given time, and the process is cleaned up by a scheduled job.

| Parameter     | Description                                                                                                                                                                                                                                                |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `errorDetail` | Possible values: <br/> `expiredProcessOnboarding` - the linked onboarding process expired <br/> `expiredProcessActivation` - the linked PowerAuth activation expired <br/> `expiredProcessIdentityVerification` - the identity verification itself expired |


### Message `Switched to COMPLETED/FAILED; user ID: {userId}`

The onboarding process failed.


### Message `Onboarding approval result: {clientApprovalResult}, resultReason: {resultReason}`

Client approval request was sent to the onboarding provider [client approve](./External-Onboarding-Services.md#onboarding-approval-service) and a response was received.

Event-specific parameters:

| Parameter      | Description                          |
|----------------|--------------------------------------|
| `resultReason` | Detail of the `clientApprovalResult` |


### Message `Onboarding approval result: FAILED`

Call to the onboarding provider [client approve](./External-Onboarding-Services.md#onboarding-approval-service) failed because of technical issues—the calling service didn't return a result.


### Message `Switched to DOCUMENT_UPLOAD/IN_PROGRESS; user ID: {userId}`

The user is expected to submit their identity document for verification. No sensitive data has been evaluated yet — the process is waiting for the user to provide evidence of identity.


### Message `Switched to DOCUMENT_UPLOAD/VERIFICATION_PENDING; user ID: {userId}`

The onboarding process is waiting for the final verification of the uploaded documents.


### Message `Switched to DOCUMENT_VERIFICATION/ACCEPTED; user ID: {userId}`

All uploaded documents satisfy the requirements for the onboarding process and are passed by evaluation with the verification provider.


### Message `Switched to DOCUMENT_VERIFICATION/FAILED; user ID: {userId}`

Document verification failed due to a technical error when calling the document verification provider.


### Message `Switched to DOCUMENT_VERIFICATION_FINAL/IN_PROGRESS; user ID: {userId}`

Final verification of the uploaded documents is performed using the evaluation returned by the provider:
- Documents belong to the same person — cross-check
- The claimed document type matches the one identified by the provider


### Message `Switched to DOCUMENT_VERIFICATION_FINAL/ACCEPTED; user ID: {userId}`

Document final verification passed and the onboarding process continues.


### Message `Switched to DOCUMENT_VERIFICATION_FINAL/REJECTED; user ID: {userId}`

Business logic rejected the documents final verification.


### Message `Switched to DOCUMENT_VERIFICATION_FINAL/FAILED; user ID: {userId}`

The documents final verification failed because of a technical issue, not business logic.


### Message `Switched to CLIENT_EVALUATION/ACCEPTED; user ID: {userId}`

Client evaluation was accepted.


### Message `Switched to CLIENT_EVALUATION/IN_PROGRESS; user ID: {userId}`

The [client evaluation service](./External-Onboarding-Services.md#client-evaluation-service) returned `WAIT` — the result is not yet available and will be delivered asynchronously.


### Message `Switched to CLIENT_EVALUATION/FAILED; user ID: {userId}`

The [client evaluation service](./External-Onboarding-Services.md#client-evaluation-service) failed due to a technical issue.


### Message `Switched to CLIENT_EVALUATION/REJECTED; user ID: {userId}`

Client evaluation was rejected by the [Client evaluation service](./External-Onboarding-Services.md#client-evaluation-service).


### Message `Switched to OTP_VERIFICATION/VERIFICATION_PENDING; user ID: {userId}`

The identity verification entered the OTP verification phase. The OTP code is about to be sent to the user.


### Message `Switched to PRESENCE_CHECK/NOT_INITIALIZED; user ID: {userId}`

The onboarding process moved to the phase, where presence check will be performed.


### Message `Switched to PRESENCE_CHECK/IN_PROGRESS; user ID: {userId}`

The presence check session with the provider is active, and the mobile client is performing the liveness check (face scan).


### Message `Switched to PRESENCE_CHECK/VERIFICATION_PENDING; user ID: {userId}`

The mobile client has completed and submitted the liveness check. The server is awaiting the provider's verdict on whether the captured biometric matches the reference image from the identity document.


### Message `Switched to PRESENCE_CHECK/ACCEPTED; user ID: {userId}`

The provider confirmed that the live biometric captured by the mobile client matches the reference image from the identity document.
The user has passed both liveness detection and face matching.


### Message `Switched to PRESENCE_CHECK/REJECTED; user ID: {userId}`

The presence check provider rejected the liveness check.


### Message `Switched to PRESENCE_CHECK/FAILED; user ID: {userId}`

The presence check failed due to a technical error.


### Message `Switched to ONBOARDING_APPROVAL/IN_PROGRESS; user ID: {userId}`

The [onboarding approval service](./External-Onboarding-Services.md#onboarding-approval-service) returned `WAIT` — the approval result is not yet available and will be delivered asynchronously.


### Message `Switched to ONBOARDING_APPROVAL/ACCEPTED; user ID: {userId}`

The [onboarding approval service](./External-Onboarding-Services.md#onboarding-approval-service) returned `OK` — the onboarding was approved and the process continues.


### Message `Switched to ONBOARDING_APPROVAL/REJECTED; user ID: {userId}`

The [onboarding approval service](./External-Onboarding-Services.md#onboarding-approval-service) returned `NOK` — the onboarding was rejected.


### Message `Switched to ONBOARDING_APPROVAL/FAILED; user ID: {userId}`

The [onboarding approval service](./External-Onboarding-Services.md#onboarding-approval-service) failed due to a technical issue.


### Message `Switched to ACTIVATION_FINISH/IN_PROGRESS; user ID: {userId}`

The final PowerAuth activation is being created — the temporary activation used during onboarding is being exchanged for the permanent one.


### Message `Switched to COMPLETED/ACCEPTED; user ID: {userId}`

All identity verification checks have been successfully passed. The user's identity is confirmed.


### Message `Process failed: {errorDetail}, for user: {userId}`

The onboarding process failed.

| Parameter     | Description             |
|---------------|-------------------------|
| `errorDetail` | Detail of the failure   |


## Event type `activation`


### Event type-specific parameters

| Parameter             | Description                                                           |
|-----------------------|-----------------------------------------------------------------------|
| `targetActivationId`  | Final PowerAuth activation ID set after successful onboarding process |


### Message `Remove activation of failed process for user: {userId}`

Activation linked to a failed onboarding process was removed during background cleanup.


### Message `Remove activation for user: {userId}`

Activation linked to the onboarding process was removed during the manual cleanup—by calling endpoint `POST /api/onboarding/cleanup`.


### Message `Create target activation for user: {userId}`

Target PowerAuth activation was created.


### Message `Remove activation for user: {userId}`

Uncommited target PowerAuth activation was removed.


## Event type `documentVerification`


### Event type-specific parameters

| Parameter                | Description                                                       |
|--------------------------|-------------------------------------------------------------------|
| `identityVerificationId` | Internal identity verification ID                                 |
| `documentVerificationId` | ID of document verification operation performed by the provider   |
| `documentId`             | Internal document ID used in the onboarding process verification  |


### Message `Expired Document verification for user: {identityVerificationId}, {errorDetail}`

Document verification wasn't completed in a given time, and the process is cleaned up by a scheduled job.

Event-specific parameters:

| Parameter     | Description                                                                                                                                                                                                                                  |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `errorDetail` | Possible values: <br/> `expired` - the document verification itself expired <br/> `expiredProcessActivation` - the linked PowerAuth activation expired <br/> `expiredProcessIdentityVerification` - the linked identity verification expired |


### Message `Document rejected because of client evaluation for user: {userId}`

Client evaluation was rejected by the [Client evaluation service](./External-Onboarding-Services.md#client-evaluation-service).


### Message `Document verification failed for user: {userId}`

Set of documents was not sent to the document verification provider because of an error (service not available, invalid document, etc.).


### Message `Document replaced with new one for user: {userId}`

A new submission replaced a previously uploaded document. The old document is marked as no longer used for verification.


### Message `Document verification failed for user: {userId}, detail: {detail}`

Document was not sent to the document verification provider because of an error (service not available, invalid document, etc.). This is logged for each document type and side.

Event-specific parameters:

| Parameter | Description            |
|-----------|------------------------|
| `detail`  | Detail of the failure  |


### Message `Document verification rejected for user: {userId}, reason: {rejectReason}`

Document was sent to the document verification provider, but the provider rejected it. This is logged for each document type and side.

Event-specific parameters:

| Parameter       | Description                 |
|-----------------|-----------------------------|
| `rejectReason`  | Detail of the reject result |


### Message `Document selfie changed status to {documentStatus} for user: {userId}`

The uploaded selfie document changed status immediately after upload processing.
This event is logged only for documents of type `SELFIE_PHOTO`.

Event-specific parameters:

| Parameter        | Description                                                                                                                      |
|------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `documentStatus` | Possible values: <br/> `VERIFICATION_PENDING` - will be verified as other document types <br/> `ACCEPTED` - no need to verify it |


### Message `Document verification pending for user: {userId}`

Document is pending for the final verification against the evaluation returned by the provider. This message is logged for each document type and side.


### Message `Document accepted at final verification for user: {userId}`

The document passed final verification. This message is logged for each document type and side.


### Message `Document rejected at final verification for user: {userId}`

The document was rejected in final verification because of business logic. This message is logged for each document type and side.


### Message `Document failed at final verification for user: {userId}`

The document failed final verification because of a technical issue. This is logged for each document type and side.


### Message `Selfie document accepted for user: {userId}`

The selfie photo document was automatically accepted without verification against the submitted identity document.


### Message `Document verification status changed to {documentVerificationStatus} for user: {userId}`

The provider returned evaluation for an individual document. This message is logged for each document type and side.


## Event type `presenceCheckProvider`


### Event type-specific parameters

| Parameter              | Description                                                                                             |
|------------------------|---------------------------------------------------------------------------------------------------------|
| `presenceCheckStatus`  | Result of presence check verification. Possible values: `IN_PROGRESS`, `ACCEPTED`, `REJECTED`, `FAILED` |


### Message `Uploaded presence check data for user: {userId}`

Liveness data was uploaded to the presence check provider by calling the `POST /api/identity/presence-check/upload` endpoint.


### Message `Got presence check result: {presenceCheckStatus} for user: {userId}`

The server received the result of the presence check verification.


### Message `Clean up presence check data for user: {userId}`

Presence check data was removed from the provider during manual cleanup by calling the `POST /api/identity/cleanup` endpoint.


### Message `Presence check initialization skipped for user: {userId}, image already uploaded`

Presence check initialization was skipped because the reference image was already uploaded in a previous attempt.


### Message `Presence check initialized for user: {userId}`

The trusted face photo was fetched from the uploaded document, upscaled if needed, and uploaded to the presence check provider.
The provider session has been prepared with the reference image.


### Message `Presence check started for user: {userId}`

A new presence check session was started with the provider.


## Event type `documentVerificationProvider`


### Message `Document verification response, user: {userId}, provider: {providerName}, documentType: {documentType}`

The provider has returned its evaluation of the submitted identity document. Note that `documentType` reflects what the user claimed via the mobile 
client — not what the provider independently determined.

Event-specific parameters:

| Parameter               | Description                                                                                                            |
|-------------------------|------------------------------------------------------------------------------------------------------------------------|
| `providerName`          | Name of the provider, currently only `Microblink` is used                                                              |
| `documentType`          | Type of document. Possible values: `ID_CARD`, `PASSPORT`, `DRIVING_LICENSE`, `SELFIE_PHOTO`, `SELFIE_VIDEO`, `UNKNOWN` |
| `documentResponseJson`  | JSON response from the document verification provider without personal data                                            |


### Message `Submit documents for user: {userId}, document IDs: {documentUploadIds}`

Documents are successfully uploaded and sent to the document verification provider. 

Event-specific parameters:

| Parameter            | Description                                                                                                          |
|----------------------|----------------------------------------------------------------------------------------------------------------------|
| `documentUploadIds`  | Comma-separated list of document IDs that were uploaded and sent to the provider. Each document side has its own ID. |


### Message `Cross verified documents: {documentVerificationStatus} for user: {userId}`

Result of the final verification for all verified documents.


### Message `Documents verified: {documentVerificationStatus} for user: {userId}`

The provider returned evaluation for an entire set of uploaded documents. The set means all documents in the request body of
the upload endpoint `POST /api/v2/identity/document/submit`.


### Message `Check document upload for user: {userId}`

A face photo of a person was fetched from a trusted source.


### Message `Sdk initialized for user: {userId}`

The mobile client document verification SDK is initialized and ready to be used for scanning identity documents.
The endpoint `POST /api/identity/document/init-sdk` was called.


### Message `Cleaned up documents for user: {userId}`

Documents were deleted from the document verification provider and server during manual cleanup by calling the `POST /api/identity/cleanup` endpoint.


## Event type `onboardingProvider`


### Message `Client evaluated for user: {userId}`

Client evaluation was successfully sent to the [Client evaluation service](./External-Onboarding-Services.md#client-evaluation-service).


### Message `Sent user verification OTP for user: {userId}`

User verification OTP was sent to the onboarding provider for the first time.


### Message `Resent user verification OTP for user: {userId}`

User verification OTP was resent to the onboarding provider.


### Message `Looked up user: {userId}`

The onboarding provider [lookup endpoint](./External-Onboarding-Services.md#user-lookup-service) found the user and returned the user ID.


### Message `Error to look up user: {userId}, {errorDetail}`

The onboarding provider [lookup endpoint](./External-Onboarding-Services.md#user-lookup-service) responded with a business logic error.

Event-specific parameters:

| Parameter     | Description                              |
|---------------|------------------------------------------|
| `errorDetail` | Error message returned by the provider.  |


### Message `Consent text approval failed for user: {userId}, error: {errorDetail}`

The onboarding provider [consent approval endpoint](./External-Onboarding-Services.md#consent-storage-service) responded with a business logic error.

| Parameter     | Description                              |
|---------------|------------------------------------------|
| `errorDetail` | Error message returned by the provider.  |


### Message `Approve consent text for user: {userId}`

The onboarding provider [consent approval endpoint](./External-Onboarding-Services.md#consent-storage-service) successfully received the user decision.


### Message `Sent activation OTP for user: {userId}`

An activation OTP code was sent for the first time by the onboarding provider [otp endpoint](./External-Onboarding-Services.md#otp-delivery-service) during onboarding start.


### Message `Resent activation OTP for user: {userId}`

An activation OTP code was resent by the onboarding provider [otp endpoint](./External-Onboarding-Services.md#otp-delivery-service) after a resend request.