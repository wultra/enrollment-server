# Audit

This feature provides audit logging for the onboarding server in the database.
For more detailed developer documentation see [auditing library documentation](https://github.com/wultra/java-core?tab=readme-ov-file#wultra-auditing-library). |


## Audit types

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


## How to search in audit logs

<!-- begin box warning -->
The audit log should be used to investigate specific onboarding processes. It should not be used for reporting, as it is not optimized for this purpose 
and would negatively impact performance.
<!-- end -->

The most frequently logged parameters are `activationId` and `userId`. Values are stored either in the `message` or `param` column of the `audit_log` table.

For example, to find all entries with activation ID `6c115802-fad3-474a-afe6-b69215740a99` following query can be used:

```sql
SELECT *
FROM audit_log
WHERE message LIKE '%6c115802-fad3-474a-afe6-b69215740a99%'
   OR param LIKE '%6c115802-fad3-474a-afe6-b69215740a99%'
order by timestamp_created desc;
```

Example of the query result for a successful onboarding process. Events are sorted chronologically—the newest first:

```csv
audit_log_id,application_name,audit_level,audit_type,timestamp_created,message,exception_message,stack_trace,param,calling_class,thread_name,version,build_time
709161b8-be44-4992-b764-feeb00271062,onboarding-server,INFO,process,2026-03-19 11:53:42.048000,Process finished for user: mockuser_567780649667363606,,,"{""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-1,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
f00d02ec-687d-48cc-b5d4-a06bed567bf8,onboarding-server,INFO,identityVerification,2026-03-19 11:53:42.032000,Switched to COMPLETED/ACCEPTED; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-1,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
0072651b-7776-4e1f-83cc-e477e764bffd,onboarding-server,INFO,identityVerification,2026-03-19 11:53:42.016000,Switched to PRESENCE_CHECK/ACCEPTED; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-1,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
d4d6ad56-5f4b-46c8-81c4-d7f886cf45af,onboarding-server,INFO,presenceCheckProvider,2026-03-19 11:53:42.013000,Got presence check result: ACCEPTED for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-1,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
9a673a2f-37bc-4216-a29d-682ba3025bf5,onboarding-server,INFO,identityVerification,2026-03-19 11:53:41.793000,Switched to PRESENCE_CHECK/VERIFICATION_PENDING; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-9,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
fce2ef6a-ab7e-4397-871f-071fe0231066,onboarding-server,INFO,identityVerification,2026-03-19 11:53:41.740000,Switched to PRESENCE_CHECK/IN_PROGRESS; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-7,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
281a931d-4fb7-4192-b7e7-3aee7fb7c3a6,onboarding-server,INFO,presenceCheckProvider,2026-03-19 11:53:41.740000,Presence check started for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-7,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
2eb16eea-b085-4cc3-b2a1-7c8b0f50275f,onboarding-server,INFO,presenceCheckProvider,2026-03-19 11:53:41.739000,Presence check initialized for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-7,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
01634d3c-9d5d-42c1-befe-9b25b431360f,onboarding-server,INFO,documentVerificationProvider,2026-03-19 11:53:41.673000,Check document upload for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-7,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
9df9b797-283f-407e-833f-4f4ffca6c874,onboarding-server,INFO,identityVerification,2026-03-19 11:53:39.024000,Switched to PRESENCE_CHECK/NOT_INITIALIZED; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,boundedElastic-1,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
4ec1039d-1d48-47f8-906c-73fbfb505fa2,onboarding-server,INFO,identityVerification,2026-03-19 11:53:36.085000,Switched to CLIENT_EVALUATION/ACCEPTED; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,boundedElastic-1,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
11f024e5-52f0-4464-a496-7f974d8f745f,onboarding-server,INFO,onboardingProvider,2026-03-19 11:53:36.072000,Client evaluated for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-1,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
6c96d8ad-a5eb-4328-9cf6-6943de2c8381,onboarding-server,INFO,documentVerification,2026-03-19 11:53:33.046000,Document accepted at final verification for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""6def8ed0-c5a7-41f9-ac1e-fb21da117b36"",""documentVerificationId"":""52612076-ae6d-44d1-ae0d-97be2615afd0""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-2,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
12e735b9-3981-44fd-9b71-475cc84aae2f,onboarding-server,INFO,identityVerification,2026-03-19 11:53:33.046000,Switched to DOCUMENT_VERIFICATION_FINAL/ACCEPTED; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-2,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
c99a636e-1e10-48d7-a46c-08916fe9638d,onboarding-server,INFO,documentVerification,2026-03-19 11:53:33.046000,Document accepted at final verification for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""4bce7301-c5b4-446c-9ab8-bf081a342975"",""documentVerificationId"":""52612076-ae6d-44d1-ae0d-97be2615afd0""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-2,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
55459e64-e1f3-4895-877b-6ea0c6211636,onboarding-server,INFO,documentVerification,2026-03-19 11:53:33.046000,Document accepted at final verification for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""220c7986-300e-4ade-b4eb-6a2c7e45730a"",""documentVerificationId"":""52612076-ae6d-44d1-ae0d-97be2615afd0""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-2,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
4732f7a8-426f-4280-8ec1-9bc62e79e058,onboarding-server,INFO,documentVerificationProvider,2026-03-19 11:53:33.044000,Cross verified documents: ACCEPTED for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-2,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
3b76a692-f5e5-4300-8625-ab819b259e6f,onboarding-server,INFO,identityVerification,2026-03-19 11:53:30.029000,Switched to DOCUMENT_VERIFICATION_FINAL/IN_PROGRESS; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-5,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
877e21a4-00bf-49fc-89d0-c2e480ce8784,onboarding-server,INFO,identityVerification,2026-03-19 11:53:27.052000,Switched to DOCUMENT_VERIFICATION/ACCEPTED; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
5c50178e-504f-4f88-b2cf-0b0d802acd60,onboarding-server,INFO,documentVerification,2026-03-19 11:53:27.039000,Document verification status changed to ACCEPTED for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""4bce7301-c5b4-446c-9ab8-bf081a342975""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
13f1088a-f026-453b-b070-5fd4c51f6c0d,onboarding-server,INFO,documentVerificationProvider,2026-03-19 11:53:27.039000,Documents verified: ACCEPTED for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
3ff0f7a0-8602-454f-a7c1-ae4be74a4d19,onboarding-server,INFO,identityVerification,2026-03-19 11:53:24.016000,Switched to DOCUMENT_UPLOAD/VERIFICATION_PENDING; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-2,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
0774f8ae-2ae7-428d-ae43-fb5a3283c575,onboarding-server,INFO,documentVerificationProvider,2026-03-19 11:53:23.406000,"Submit documents for user: mockuser_567780649667363606, document IDs: [4bce7301-c5b4-446c-9ab8-bf081a342975]",,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-9,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
db4017f0-2c78-4067-916b-6d3844d92b2b,onboarding-server,INFO,documentVerification,2026-03-19 11:53:23.406000,Document verification pending for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""4bce7301-c5b4-446c-9ab8-bf081a342975""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-9,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
2afe32da-1aac-42fa-a124-db0c92578c9d,onboarding-server,INFO,onboardingProvider,2026-03-19 11:53:23.384000,"Document verification response, user: mockuser_567780649667363606, provider: Microblink, documentType: DRIVING_LICENSE",,,"{""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentResponseJson"":""{\""processingStatus\"":\""Completed\"",\""verification\"":{\""certaintyLevel\"":\""High\"",\""recommendedOutcome\"":\""Accept\"",\""type\"":\""DetailedCheck\"",\""result\"":\""Pass\"",\""performedChecks\"":10},\""checks\"":[{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""ExtractedDataCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""MatchCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""field\"":\""FirstName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""LastName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""FullName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Address\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PlaceOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Race\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Religion\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Profession\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""MaritalStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ResidentialStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Employer\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfIssue\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfExpiry\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentOptionalAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""LogicCheck\"",\""result\"":\""Pass\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""DateLogicCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""DateOfBirthBeforeDateOfIssueCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfBirthBeforeDateOfExpiryCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfIssueBeforeDateOfExpiryCheck\"",\""result\"":\""Fail\""},{\""type\"":\""Check\"",\""name\"":\""DateOfBirthInPastCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfIssueInPastCheck\"",\""result\"":\""Fail\""}]},{\""type\"":\""Check\"",\""name\"":\""DocumentNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""PersonalIdNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""InventoryControlNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""DocumentDiscriminatorLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""CustomerIdNumberLogic\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""FieldFormatCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""field\"":\""DateOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfExpiry\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfIssue\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentOptionalAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""AdditionalPersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Sex\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Nationality\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""IssuingAuthority\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""MaritalStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Religion\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ClassEffectiveDate\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ClassExpiryDate\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""}]},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""BarcodeAuthenticity\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""ContentCheck\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""ReadCheck\"",\""result\"":\""NotPerformed\""}]},{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SuspiciousDataCheck\"",\""result\"":\""Pass\"",\""checks\"":[{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SuspiciousNumberCheck\"",\""result\"":\""Pass\""},{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SampleStringCheck\"",\""result\"":\""Pass\""}]},{\""type\"":\""Check\"",\""name\"":\""DataIntegrityCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""MRZCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""Parsed\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""CheckDigits\"",\""result\"":\""NotPerformed\""}]}]},{\""type\"":\""Check\"",\""name\"":\""DocumentLivenessCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""ScreenCheck\"",\""result\"":\""NotPerformed\""},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""PhotocopyCheck\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""VisualCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""PhotoForgeryCheck\"",\""result\"":\""NotPerformed\""},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""SecurityFeatures\"",\""result\"":\""NotPerformed\"",\""details\"":{}}]},{\""type\"":\""Check\"",\""name\"":\""DocumentValidityCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""VersionCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""ExpiredCheck\"",\""result\"":\""Fail\""}]}],\""processIndicators\"":[{\""name\"":\""Clarity\"",\""type\"":\""ImageQuality\"",\""result\"":\""Pass\""},{\""name\"":\""HandPresence\"",\""type\"":\""ScanProcess\"",\""result\"":\""Fail\""},{\""name\"":\""Cropped\"",\""type\"":\""ImageQuality\"",\""result\"":\""Pass\""}],\""messages\"":[{\""code\"":\""I001\"",\""message\"":\""We've extracted and completed all feasible actions. If the result is inadequate, review the process indicators.\"",\""status\"":\""Info\""}],\""runtime\"":{\""startedOn\"":\""2026-02-01T19:13:58.2391777Z\"",\""finishedOn\"":\""2026-02-01T19:13:59.4174137Z\"",\""elapsedMs\"":1178,\""serviceVersion\"":\""3.17.2\"",\""runnerVersion\"":\""unknown\"",\""runnerInstanceKey\"":\""unknown\"",\""runnerInstanceIndex\"":0,\""wrapperVersion\"":\""3.18.0-amd64\"",\""extractionRecognizerVersion\"":\""17.0.6\"",\""verificationRecognizerVersion\"":\""16.0.1\"",\""clientSdkName\"":\""\"",\""clientSdkVersion\"":\""\"",\""traceId\"":\""00-1f9f307f56322e931e9ebbc771108236-60606cd7b9a2337b-01\""},\""optionsUsed\"":{\""screenMatchLevel\"":\""Disabled\"",\""photocopyMatchLevel\"":\""Disabled\"",\""barcodeAnomalyMatchLevel\"":\""Disabled\"",\""photoForgeryMatchLevel\"":\""Disabled\"",\""staticSecurityFeaturesMatchLevel\"":\""Disabled\"",\""dataMatchMatchLevel\"":\""Disabled\"",\""blurMatchLevel\"":\""Disabled\"",\""glareMatchLevel\"":\""Disabled\"",\""lightingMatchLevel\"":\""Disabled\"",\""sharpnessMatchLevel\"":\""Disabled\"",\""handOcclusionMatchLevel\"":\""Disabled\"",\""dpiMatchLevel\"":\""Disabled\"",\""tiltMatchLevel\"":\""Disabled\"",\""imageQualityInterpretation\"":\""Conservative\"",\""sideMode\"":\""MultiSide\"",\""treatExpirationAsFraud\"":false},\""useCaseUsed\"":{\""documentVerificationPolicy\"":\""Standard\"",\""verificationContext\"":\""InPerson\"",\""manualReviewStrategy\"":\""Never\"",\""manualReviewSensitivity\"":\""Default\"",\""captureConditions\"":\""Basic\""}}""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-9,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
c3901546-97a8-451c-9521-4f1b2955235e,onboarding-server,INFO,identityVerification,2026-03-19 11:53:18.071000,Switched to DOCUMENT_UPLOAD/IN_PROGRESS; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
5a5bdcc0-3a86-4b56-8699-7425ca606b5c,onboarding-server,INFO,documentVerification,2026-03-19 11:53:18.062000,Document verification status changed to ACCEPTED for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""220c7986-300e-4ade-b4eb-6a2c7e45730a""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
cc909550-55a9-49a8-8ef6-f70cba6385d7,onboarding-server,INFO,documentVerification,2026-03-19 11:53:18.059000,Document verification status changed to ACCEPTED for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""6def8ed0-c5a7-41f9-ac1e-fb21da117b36""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
8bd7b930-1200-4a86-b34c-56d6fd82967e,onboarding-server,INFO,documentVerificationProvider,2026-03-19 11:53:18.058000,Documents verified: ACCEPTED for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
6518410f-be85-4d2f-ab30-3f898145410c,onboarding-server,INFO,identityVerification,2026-03-19 11:53:15.023000,Switched to DOCUMENT_UPLOAD/VERIFICATION_PENDING; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,scheduling-2,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
c8b92d20-3bd6-42a4-b869-5fe080fb1877,onboarding-server,INFO,documentVerificationProvider,2026-03-19 11:53:14.061000,"Submit documents for user: mockuser_567780649667363606, document IDs: [6def8ed0-c5a7-41f9-ac1e-fb21da117b36, 220c7986-300e-4ade-b4eb-6a2c7e45730a]",,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
b25f68ed-16bb-4a49-8c76-d87e5cc1ff2d,onboarding-server,INFO,documentVerification,2026-03-19 11:53:14.061000,Document verification pending for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""220c7986-300e-4ade-b4eb-6a2c7e45730a""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
e5a9284c-ba19-449a-8a7e-3b686f7f8029,onboarding-server,INFO,documentVerification,2026-03-19 11:53:14.061000,Document verification pending for user: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""6def8ed0-c5a7-41f9-ac1e-fb21da117b36""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
69a57721-6fc3-4ea3-adf7-380ad1ab2d96,onboarding-server,INFO,onboardingProvider,2026-03-19 11:53:14.035000,"Document verification response, user: mockuser_567780649667363606, provider: Microblink, documentType: ID_CARD",,,"{""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentResponseJson"":""{\""processingStatus\"":\""Completed\"",\""verification\"":{\""certaintyLevel\"":\""High\"",\""recommendedOutcome\"":\""Accept\"",\""type\"":\""DetailedCheck\"",\""result\"":\""Pass\"",\""performedChecks\"":10},\""checks\"":[{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""ExtractedDataCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""MatchCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""field\"":\""FirstName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""LastName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""FullName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Address\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PlaceOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Race\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Religion\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Profession\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""MaritalStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ResidentialStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Employer\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfIssue\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfExpiry\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentOptionalAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""LogicCheck\"",\""result\"":\""Pass\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""DateLogicCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""DateOfBirthBeforeDateOfIssueCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfBirthBeforeDateOfExpiryCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfIssueBeforeDateOfExpiryCheck\"",\""result\"":\""Fail\""},{\""type\"":\""Check\"",\""name\"":\""DateOfBirthInPastCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfIssueInPastCheck\"",\""result\"":\""Fail\""}]},{\""type\"":\""Check\"",\""name\"":\""DocumentNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""PersonalIdNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""InventoryControlNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""DocumentDiscriminatorLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""CustomerIdNumberLogic\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""FieldFormatCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""field\"":\""DateOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfExpiry\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfIssue\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentOptionalAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""AdditionalPersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Sex\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Nationality\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""IssuingAuthority\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""MaritalStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Religion\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ClassEffectiveDate\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ClassExpiryDate\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""}]},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""BarcodeAuthenticity\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""ContentCheck\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""ReadCheck\"",\""result\"":\""NotPerformed\""}]},{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SuspiciousDataCheck\"",\""result\"":\""Pass\"",\""checks\"":[{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SuspiciousNumberCheck\"",\""result\"":\""Pass\""},{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SampleStringCheck\"",\""result\"":\""Pass\""}]},{\""type\"":\""Check\"",\""name\"":\""DataIntegrityCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""MRZCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""Parsed\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""CheckDigits\"",\""result\"":\""NotPerformed\""}]}]},{\""type\"":\""Check\"",\""name\"":\""DocumentLivenessCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""ScreenCheck\"",\""result\"":\""NotPerformed\""},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""PhotocopyCheck\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""VisualCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""PhotoForgeryCheck\"",\""result\"":\""NotPerformed\""},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""SecurityFeatures\"",\""result\"":\""NotPerformed\"",\""details\"":{}}]},{\""type\"":\""Check\"",\""name\"":\""DocumentValidityCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""VersionCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""ExpiredCheck\"",\""result\"":\""Fail\""}]}],\""processIndicators\"":[{\""name\"":\""Clarity\"",\""type\"":\""ImageQuality\"",\""result\"":\""Pass\""},{\""name\"":\""HandPresence\"",\""type\"":\""ScanProcess\"",\""result\"":\""Fail\""},{\""name\"":\""Cropped\"",\""type\"":\""ImageQuality\"",\""result\"":\""Pass\""}],\""messages\"":[{\""code\"":\""I001\"",\""message\"":\""We've extracted and completed all feasible actions. If the result is inadequate, review the process indicators.\"",\""status\"":\""Info\""}],\""runtime\"":{\""startedOn\"":\""2026-02-01T19:13:58.2391777Z\"",\""finishedOn\"":\""2026-02-01T19:13:59.4174137Z\"",\""elapsedMs\"":1178,\""serviceVersion\"":\""3.17.2\"",\""runnerVersion\"":\""unknown\"",\""runnerInstanceKey\"":\""unknown\"",\""runnerInstanceIndex\"":0,\""wrapperVersion\"":\""3.18.0-amd64\"",\""extractionRecognizerVersion\"":\""17.0.6\"",\""verificationRecognizerVersion\"":\""16.0.1\"",\""clientSdkName\"":\""\"",\""clientSdkVersion\"":\""\"",\""traceId\"":\""00-1f9f307f56322e931e9ebbc771108236-60606cd7b9a2337b-01\""},\""optionsUsed\"":{\""screenMatchLevel\"":\""Disabled\"",\""photocopyMatchLevel\"":\""Disabled\"",\""barcodeAnomalyMatchLevel\"":\""Disabled\"",\""photoForgeryMatchLevel\"":\""Disabled\"",\""staticSecurityFeaturesMatchLevel\"":\""Disabled\"",\""dataMatchMatchLevel\"":\""Disabled\"",\""blurMatchLevel\"":\""Disabled\"",\""glareMatchLevel\"":\""Disabled\"",\""lightingMatchLevel\"":\""Disabled\"",\""sharpnessMatchLevel\"":\""Disabled\"",\""handOcclusionMatchLevel\"":\""Disabled\"",\""dpiMatchLevel\"":\""Disabled\"",\""tiltMatchLevel\"":\""Disabled\"",\""imageQualityInterpretation\"":\""Conservative\"",\""sideMode\"":\""MultiSide\"",\""treatExpirationAsFraud\"":false},\""useCaseUsed\"":{\""documentVerificationPolicy\"":\""Standard\"",\""verificationContext\"":\""InPerson\"",\""manualReviewStrategy\"":\""Never\"",\""manualReviewSensitivity\"":\""Default\"",\""captureConditions\"":\""Basic\""}}""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-4,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
8da113bd-dbeb-4c35-9e41-4d88e10e7628,onboarding-server,INFO,identityVerification,2026-03-19 11:53:13.818000,Switched to DOCUMENT_UPLOAD/IN_PROGRESS; user ID: mockuser_567780649667363606,,,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}",com.wultra.app.onboardingserver.common.service.AuditService,http-nio-8083-exec-3,2.1.0-SNAPSHOT,2026-03-19 11:51:59.726000
```


### Parameters

Here is description of the parameters used in the `audit_log.param` and `audit_log.message` columns:

| Parameter                    | Description                                                                                                            |
|------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `identityVerificationId`     | Internal identity verification ID                                                                                      |
| `processId`                  | ID of the onboarding process                                                                                           |
| `activationId`               | PowerAuth activation ID bind with the onboarding process                                                               |
| `targetActivationId`         | Final PowerAuth activation ID set after successful onboarding process                                                  |
| `userId`                     | ID of user passing the onboarding process                                                                              |
| `otpId`                      | ID of the OTP used in the onboarding process step                                                                      |
| `documentId`                 | Internal document ID used in the onboarding process verification                                                       |
| `documentVerificationId`     | ID of document verification operation                                                                                  |
| `documentResponseJson`       | JSON response from the document verification provider                                                                  |
| `providerName`               | Document verification provider name                                                                                    |
| `documentType`               | Type of document. Possible values: `ID_CARD`, `PASSPORT`, `DRIVING_LICENSE`, `SELFIE_PHOTO`, `SELFIE_VIDEO`, `UNKNOWN` |
| `documentUploadId`           | ID of document upload operation. Each document side has its own ID.                                                    |
| `documentVerificationStatus` | Status of document verification. Possible values: `IN_PROGRESS`, `ACCEPTED`, `REJECTED`, `FAILED`                      |
| `presenceCheckStatus`        | Result of presence check verification. Possible values: `IN_PROGRESS`, `ACCEPTED`, `REJECTED`, `FAILED`                |
| `errorDetail`                | Detail of an error                                                                                                     |
| `clientApprovalResult`       | Result of client approval. Possible values: `OK`, `NOK`, `WAIT`                                                        |
| `clientApprovalReason`       | Detail of the `clientApprovalResult`                                                                                   |
| `clientEvaluationResult`     | Result of client evaluation. Possible values: `OK`, `NOK`, `WAIT`                                                      |
| `otpType`                    | Type of OTP. Possible values: `ACTIVATION`, `USER_VERIFICATION`                                                        |


### Events

Description of the events logged in the `audit_log` table.


#### Onboarding started

The onboarding process was created and started. The endpoint `POST api/onboarding/start` was called and existing process was not found for the provided user identification data.

MESSAGE: `Process started for user: {userId}`


#### Onboarding resumed

The onboarding process already exists and was resumed. The endpoint `POST api/onboarding/resume` was called and existing process was found for the provided user identification data.


#### Activation OTP sent

An activation OTP code was delivered by the onboarding provider [otp endpoint](./External-Onboarding-Services.md#otp-delivery-service) during onboarding start.

MESSAGE: `Sent activation OTP for user: {userId}`


#### Activation OTP resent

An activation OTP code was resent by the onboarding provider [otp endpoint](./External-Onboarding-Services.md#otp-delivery-service) after a resend request.

MESSAGE: `Resent activation OTP for user: {userId}`


#### User looked up by identification

The onboarding provider [lookup endpoint](./External-Onboarding-Services.md#user-lookup-service) found the user and returned the user ID.

MESSAGE: `Looked up user: {userId}`


#### User lookup failed

The onboarding provider [lookup endpoint](./External-Onboarding-Services.md#user-lookup-service) responded with an error.

MESSAGE: `Error to look up user: {userId}, {errorDetail}`


#### Consent text approved

The onboarding provider [consent approval endpoint](./External-Onboarding-Services.md#consent-storage-service) successfully stored the user decision.

MESSAGE: `Approve consent text for user: {userId}`


#### Consent text approval failed

The onboarding provider [consent approval endpoint](./External-Onboarding-Services.md#consent-storage-service) responded with an error.

MESSAGE: `Consent text approval failed for user: {userId}, error: {errorDetail}`


#### Documents are uploaded

The user is expected to submit their identity document for verification. No sensitive data has been evaluated yet — the process is waiting for the user to provide evidence of identity.

MESSAGE: `Switched to DOCUMENT_UPLOAD/IN_PROGRESS; user ID: {userId}`


#### Verification SDK initialized

The mobile client document verification SDK is initialized and ready to be used for scanning identity documents.
The endpoint `POST /api/identity/document/init-sdk` was called.

MESSAGE: `Sdk initialized for user: {userId}`


#### Documents submitted to the provider

Documents are successfully uploaded and sent to the document verification provider.

MESSAGE: `Submit documents for user: {userId}, document IDs: [{documentUploadIds}]`


#### Documents submit failed

Set of documents was not sent to the document verification provider because of an error (service not available, invalid document, etc.)

MESSAGE: `Document verification failed for user: {userId}`


#### Document submit failed

Document was not sent to the document verification provider because of an error (service not available, invalid document, etc.). This is logged for each document type and side.
This is related to this [event](#documents-submit-failed), which is logged for the entire set of documents.

MESSAGE: `Document verification failed for user: {userId}, detail: {detail}`


#### Document resubmitted

A previously uploaded document was replaced by a new submission. The old document is marked as no longer used for verification.

MESSAGE: `Document replaced with new one for user: {userId}`


#### Document rejected by the provider

Document was sent to the document verification provider, but the provider rejected it. This is logged for each document type and side.

MESSAGE: `Document verification rejected for user: {userId}, reason: {documentRejectReason}`


#### Document evaluation result received from the provider

The provider has returned its evaluation of the submitted identity document. Note that `documentType` reflects what the user claimed via the mobile client — not what the provider independently determined.

MESSAGE: `Document verification response, user: {userId}, provider: {providerName}, documentType: {documentType}`


#### Document pending for final verification

Document is pending for the final verification against the evaluation returned by the provider. This message is logged for each document type and side.

MESSAGE: `Document verification pending for user: {userId}`


#### Selfie document status changed

The uploaded selfie document changed status immediately after upload processing.
This event is logged only for documents of type `SELFIE_PHOTO`.

MESSAGE: `Document selfie changed status to {selfieDocumentStatus} for user: {userId}`

Possible values of `selfieDocumentStatus`:
- `VERIFICATION_PENDING` when it will be verified as other document types
- `ACCEPTED` when there is no need to verify it


#### Onboarding process is pending for the documents final verification

The onboarding process is waiting for the final verification of the uploaded documents.

This is related to this [event](#document-pending-for-final-verification), which is logged for each document type and side.

MESSAGE: `Switched to DOCUMENT_UPLOAD/VERIFICATION_PENDING; user ID: {userId}`


#### Documents verified with the verification provider

The provider returned evaluation for an entire set of uploaded documents. The set means all documents in the request body of
the upload endpoint `POST /api/v2/identity/document/submit`. The result for an entire set is stored in `documentVerificationStatus` parameter.

MESSAGE: `Documents verified: {documentVerificationStatus} for user: {userId}`


#### Document evaluated with the verification provider

The provider returned evaluation for an individual document. This message is logged for each document type and side.
See `documentId` parameter to identify the document and `documentVerificationStatus` for the result.

This is related to this [event](#documents-verified-with-the-verification-provider), which is logged for the entire set of documents.

MESSAGE: `Document verification status changed to {documentVerificationStatus} for user: {userId}`


#### Required documents uploaded and successfully evaluated

All uploaded documents satisfy the requirements for the onboarding process and are passed by evaluation with the verification provider.

MESSAGE: `Switched to DOCUMENT_VERIFICATION/ACCEPTED; user ID: {userId}`


#### Document verification failed

Document verification failed due to a technical error when calling the document verification provider.

MESSAGE: `Switched to DOCUMENT_VERIFICATION/FAILED; user ID: {userId}`


#### Selfie document accepted

The selfie photo document was automatically accepted without verification against the submitted identity document.

MESSAGE: `Selfie document accepted for user: {userId}`


#### Document final verification is performed

Final verification of the uploaded documents is performed using the evaluation returned by the provider:
- Documents belong to the same person — cross-check
- The claimed document type matches the one identified by the provider

MESSAGE: `Switched to DOCUMENT_VERIFICATION_FINAL/IN_PROGRESS; user ID: {userId}`


#### Result of the final documents verification

Result of the documents final verification.

MESSAGE: `Cross verified documents: {documentVerificationStatus} for user: {userId}`


#### Document accepted in the final verification

The document passed final verification. This message is logged for each document type and side.
This is related to this [event](#result-of-the-final-documents-verification), which is logged for all documents.

MESSAGE: `Document accepted at final verification for user: {userId}`


#### Document rejected in the final verification

The document was rejected in final verification because of business logic. This message is logged for each document type and side.

MESSAGE: `Document rejected at final verification for user: {userId}`


#### Document failed in the final verification

The document failed final verification because of a technical issue. This is logged for each document type and side.

MESSAGE: `Document failed at final verification for user: {userId}`

#### Documents final verification passed

Document final verification passed and the onboarding process continues.

MESSAGE: `Switched to DOCUMENT_VERIFICATION_FINAL/ACCEPTED; user ID: {userId}`


#### Documents final verification rejected

Business logic rejected the documents final verification.

MESSAGE: `Switched to DOCUMENT_VERIFICATION_FINAL/REJECTED; user ID: {userId}`


#### Documents final verification failed

The documents final verification failed because of a technical issue, not business logic.

MESSAGE: `Switched to DOCUMENT_VERIFICATION_FINAL/FAILED; user ID: {userId}`


#### Client evaluation processed

Client evaluation was successfully sent to the [Client evaluation service](./External-Onboarding-Services.md#client-evaluation-service).

MESSAGE: `Client evaluated for user: {userId}`


#### Client evaluation accepted

Client evaluation was accepted.

MESSAGE: `Switched to CLIENT_EVALUATION/ACCEPTED; user ID: {userId}`


#### Client evaluation in progress

The [client evaluation service](./External-Onboarding-Services.md#client-evaluation-service) returned `WAIT` — the result is not yet available and will be delivered asynchronously.

MESSAGE: `Switched to CLIENT_EVALUATION/IN_PROGRESS; user ID: {userId}`


#### Client evaluation failed

The [client evaluation service](./External-Onboarding-Services.md#client-evaluation-service) failed due to a technical issue.

MESSAGE: `Switched to CLIENT_EVALUATION/FAILED; user ID: {userId}`


#### Client evaluation rejected

Client evaluation was rejected by the [Client evaluation service](./External-Onboarding-Services.md#client-evaluation-service).

MESSAGE: `Document rejected because of client evaluation for user: {userId}`

MESSAGE: `Switched to CLIENT_EVALUATION/REJECTED; user ID: {userId}`


#### Client evaluation acknowledged

Client evaluation result was received on the endpoint `POST /api/private/client/evaluate`. The endpoint is called when evaluation is handled asynchronously.

MESSAGE: `Acknowledged evaluation approval result: {clientEvaluationResult}`


#### OTP verification pending

The identity verification entered the OTP verification phase. The OTP code is about to be sent to the user.

MESSAGE: `Switched to OTP_VERIFICATION/VERIFICATION_PENDING; user ID: {userId}`


#### User verification OTP sent or resent

User verification OTP was delivered by the onboarding provider.
The same event is used for initial sending and resend.

MESSAGE: `Sent user verification OTP for user: {userId}`

MESSAGE: `Resent user verification OTP for user: {userId}`


#### OTP expired

An OTP code was submitted after its expiration time and could no longer be verified.

MESSAGE: `OTP expired for user: {userId}`


#### OTP verified passed

The user submitted valid OTP code.

MESSAGE: `OTP {otpType} verified for user: {userId}`


#### OTP verification failed

The user submitted invalid OTP code.

MESSAGE: `OTP {otpType} verification failed for user: {userId}`


#### OTP max attempts reached

Too many OTP verification attempts were made.

MESSAGE: `OTP max attempts reached for user: {userId}`


#### OTP verification failed due to process state

The onboarding process is failed and the OTP verification is not possible.

MESSAGE: `OTP failed because of failed process for user: {userId}`


#### OTP failed

Previously verified user verification OTP was invalidated and marked as failed. This happens when the OTP step succeeds,
but the combined SCA evaluation fails because the earlier presence check result was not successful.

MESSAGE: `OTP failed for user: {userId}`


#### OTP resend

The user requested to resend the OTP code.

MESSAGE: `Resending OTP for user: {userId}`


#### OTP cancelled

Invalidate the OTP code. This is done by calling the endpoint `POST /api/onboarding/cleanup`.

MESSAGE: `OTP canceled for process ID: {processId}, user ID: {userId}, otp type: {otpType}`


#### Presence check phase reached

The onboarding process moved to the phase, where presence check is performed.

MESSAGE: `Switched to PRESENCE_CHECK/NOT_INITIALIZED; user ID: {userId}`


#### Photo for presence check fetched

A face photo of a person was fetched from a trusted source.

MESSAGE: `Check document upload for user: {userId}`


#### Presence check initialized

The trusted face photo was fetched from the uploaded document, upscaled if needed, and uploaded to the presence check provider.
The provider session has been prepared with the reference image.

MESSAGE: `Presence check initialized for user: {userId}`


#### Presence check initialization skipped

Presence check initialization was skipped because the reference image was already uploaded in a previous attempt.

MESSAGE: `Presence check initialization skipped for user: {userId}, image already uploaded`


#### Presence check started

A new presence check session was started with the provider.

MESSAGE: `Presence check started for user: {userId}`


#### Presence check data uploaded

Liveness data was uploaded to the presence check provider by calling the `POST /api/identity/presence-check/upload` endpoint.

MESSAGE: `Uploaded presence check data for user: {userId}`


#### Presence check data cleaned up

Presence check data was removed from the provider during manual cleanup by calling the `POST /api/identity/cleanup` endpoint.

MESSAGE: `Clean up presence check data for user: {userId}`


#### Documents cleaned up from verification provider

Documents were deleted from the document verification provider and server during manual cleanup by calling the `POST /api/identity/cleanup` endpoint.

MESSAGE: `Cleaned up documents for user: {userId}`


#### Presence check in progress phase reached

The presence check session with the provider is active, and the mobile client is performing the liveness check (face scan).

MESSAGE: `Switched to PRESENCE_CHECK/IN_PROGRESS; user ID: {userId}`


#### Presence check verification pending phase reached

The mobile client has completed and submitted the liveness check. The server is awaiting the provider's verdict on whether the captured biometric matches the reference image from the identity document.

MESSAGE: `Switched to PRESENCE_CHECK/VERIFICATION_PENDING; user ID: {userId}`


#### Presence check result received

The server received the result of the presence check verification. The result is stored in `presenceCheckStatus` parameter.

MESSAGE: `Got presence check result: {presenceCheckStatus} for user: {userId}`


#### Presence check accepted

The provider confirmed that the live biometric captured by the mobile client matches the reference image from the identity document.
The user has passed both liveness detection and face matching.

MESSAGE: `Switched to PRESENCE_CHECK/ACCEPTED; user ID: {userId}`


#### Presence check rejected

The presence check provider rejected the liveness check.

MESSAGE: `Switched to PRESENCE_CHECK/REJECTED; user ID: {userId}`


#### Presence check failed

The presence check failed due to a technical error.

MESSAGE: `Switched to PRESENCE_CHECK/FAILED; user ID: {userId}`


#### Presence check too many attempts reached

Too many attempts were made to verify the presence check.

MESSAGE: `Presence check max failed attempts reached for user: {userId}`


#### Client approval result

Client approval request was sent to the onboarding provider [client approve](./External-Onboarding-Services.md#onboarding-approval-service) and a response was received.

MESSAGE: `Onboarding approval result: {clientApprovalResult}, resultReason: {clientApprovalReason}`


#### Client approval failed

Call to the onboarding provider [client approve](./External-Onboarding-Services.md#onboarding-approval-service) failed because of technical issues—the calling service didn't return a result.

MESSAGE: `Onboarding approval result: FAILED`


#### Client approval acknowledged

Client approval result was received on the endpoint `POST /api/private/client/approve`. The endpoint is called when approval is handled asynchronously.

MESSAGE: `Acknowledged onboarding approval result: {clientApprovalResult}`


#### Onboarding approval in progress

The [onboarding approval service](./External-Onboarding-Services.md#onboarding-approval-service) returned `WAIT` — the approval result is not yet available and will be delivered asynchronously.

MESSAGE: `Switched to ONBOARDING_APPROVAL/IN_PROGRESS; user ID: {userId}`


#### Onboarding approval accepted

The [onboarding approval service](./External-Onboarding-Services.md#onboarding-approval-service) returned `OK` — the onboarding was approved and the process continues.

MESSAGE: `Switched to ONBOARDING_APPROVAL/ACCEPTED; user ID: {userId}`


#### Onboarding approval rejected

The [onboarding approval service](./External-Onboarding-Services.md#onboarding-approval-service) returned `NOK` — the onboarding was rejected.

MESSAGE: `Switched to ONBOARDING_APPROVAL/REJECTED; user ID: {userId}`


#### Onboarding approval failed

The [onboarding approval service](./External-Onboarding-Services.md#onboarding-approval-service) failed due to a technical issue.

MESSAGE: `Switched to ONBOARDING_APPROVAL/FAILED; user ID: {userId}`


#### Activation finish in progress

The final PowerAuth activation is being created — the temporary activation used during onboarding is being exchanged for the permanent one.

MESSAGE: `Switched to ACTIVATION_FINISH/IN_PROGRESS; user ID: {userId}`


#### Onboarding process passed

All identity verification checks have been successfully passed. The user's identity is confirmed.

MESSAGE: `Switched to COMPLETED/ACCEPTED; user ID: {userId}`


#### Onboarding process finished

The onboarding process was successfully finished.

MESSAGE: `Process finished for user: {userId}`


#### Onboarding process failed

The onboarding process failed.

MESSAGE: `Process failed: {processFailErrorDetail}, for user: {userId}`


#### PowerAuth activation removed for a failed process

Activation linked to a failed onboarding process was removed during background cleanup.

MESSAGE: `Remove activation of failed process for user: {userId}`


#### Target PowerAuth activation removed

Uncommited target PowerAuth activation was removed.

MESSAGE: `Remove activation for user: {userId}`


#### Target PowerAuth activation created

Target PowerAuth activation was created.

MESSAGE: `Create target activation for user: {userId}`


#### PowerAuth activation removed

Activation linked to the onboarding process was removed during the manual cleanup—by calling endpoint `POST /api/onboarding/cleanup`.

MESSAGE: `Remove activation for user: {userId}`


#### Onboarding process failed

The onboarding process failed.

MESSAGE: `Switched to COMPLETED/FAILED; user ID: {userId}`


#### Identity verification expired and cleaned up

Identity verification wasn't completed in a given time, and the process is cleaned up by a scheduled job.

MESSAGE: `Expired identity verification for user: {userId}, {expiredIdentityVerificationErrorDetail}`

Values of `expiredIdentityVerificationErrorDetail`:
- `expiredProcessOnboarding` - the linked onboarding process expired
- `expiredProcessActivation` - the linked PowerAuth activation expired
- `expiredProcessIdentityVerification` - the identity verification itself expired


#### Document verification expired and cleaned up

Document verification wasn't completed in a given time, and the process is cleaned up by a scheduled job.

MESSAGE: `Expired Document verification for user: {identityVerificationId}, {expiredDocumentVerificationErrorDetail}`

Values of `expiredDocumentVerificationErrorDetail`:
- `expired` - the document verification itself expired
- `expiredProcessActivation` - the linked PowerAuth activation expired
- `expiredProcessIdentityVerification` - the linked identity verification expired

#### OTP expired and cleaned up

The OTP was not used in a given time, and the process is cleaned up by a scheduled job.

MESSAGE: `Expired OTP for user: {userId}`


#### Process expired and cleaned up

The onboarding process was not completed in a given time, and the process is cleaned up by a scheduled job.

MESSAGE: `Expired process for user: {userId}, {expiredProcessErrorDetail}`

Values of `expiredProcessErrorDetail`:
- `expiredProcessOnboarding` - the onboarding process itself expired
- `expiredProcessActivation` - the linked PowerAuth activation expired
- `expiredProcessIdentityVerification` - the linked identity verification expired


#### Identity verification limit reached

Too many identity verifications were attempted.

MESSAGE: `Max failed attempts reached for identity verification for user: {userId}`


#### Process limit reached

Maximum number of processes per day reached.

MESSAGE: `Maximum number of processes per day reached for user: {userId}`


#### Process cleaned up

The onboarding process was cleaned up manually by calling the endpoint `POST /api/onboarding/cleanup`.

MESSAGE: `Process cleaned up for user: {userId}`
