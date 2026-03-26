# Audit

This feature provides audit logging for the onboarding server in the database.
For more detailed developer documentation see [auditing library documentation](https://github.com/wultra/java-core?tab=readme-ov-file#wultra-auditing-library).


## Database structure

The auditing library uses the following database structure. 

If a value longer than the character limit is passed to a column, it is truncated to the limit.


## Configuration

The following application properties can be used to configure audit logging:

| Property Name                                    | Default Value        | Description                                                                                         |
|--------------------------------------------------|----------------------|-----------------------------------------------------------------------------------------------------|
| `spring.application.name`                        | `onboarding-server`  | The name of application used to set `application_name` column                                       |
| `audit.level`                                    | `INFO`               | Threshold of log message levels to be persisted. See the [Audit levels](#audit-levels) section.     |
| `audit.event.queue.size`                         | `100000`             | Size of the internal queue for log entries to be written.                                           |
| `audit.db.cleanup.days`                          | `365`                | Data retention period in the database.                                                              |
| `audit.db.table.log.name`                        | `audit_log`          | Name of the database table for audit log entries.                                                   |
| `audit.db.table.param.enabled`                   | `false`              | Whether values from the column `audit_log.param` are parsed and persisted into `audit_param` table. |
| `audit.db.table.param.name`                      | `audit_param`        | Name of the database table for audit log parameters.                                                |
| `spring.jpa.properties.hibernate.default_schema` | _empty_              | The database schema to use.                                                                         |
| `audit.db.batch.size`                            | `1000`               | Batch size for database operations.                                                                 |


## Audit types

A value in the `audit_log.audit_type` column is used to categorize the audit log entry according to the operation scope.
The following values are used:

| Value                          | Description                                                         |
|--------------------------------|---------------------------------------------------------------------|
| `process`                      | General onboarding process operation                                |
| `otp`                          | OTP operations                                                      |
| `identityVerification`         | Identity verification operations                                    |
| `activation`                   | Activation operations                                               |
| `documentVerification`         | Document verification operations                                    |
| `presenceCheckProvider`        | Presence check operations                                           |
| `documentVerificationProvider` | Document verification provider operations                           |
| `onboardingProvider`           | [Onboarding provider](./External-Onboarding-Services.md) operations |


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
timestamp_created,application_name,audit_type,audit_type,message,param
2026-03-19 11:53:42.048000,onboarding-server,process,process,Process finished for user: mockuser_567780649667363606,"{""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:42.032000,onboarding-server,identityVerification,identityVerification,Switched to COMPLETED/ACCEPTED; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:42.016000,onboarding-server,identityVerification,identityVerification,Switched to PRESENCE_CHECK/ACCEPTED; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:42.013000,onboarding-server,presenceCheckProvider,presenceCheckProvider,Got presence check result: ACCEPTED for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:41.793000,onboarding-server,identityVerification,identityVerification,Switched to PRESENCE_CHECK/VERIFICATION_PENDING; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:41.740000,onboarding-server,identityVerification,identityVerification,Switched to PRESENCE_CHECK/IN_PROGRESS; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:41.740000,onboarding-server,presenceCheckProvider,presenceCheckProvider,Presence check started for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:41.739000,onboarding-server,presenceCheckProvider,presenceCheckProvider,Presence check initialized for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:41.673000,onboarding-server,documentVerificationProvider,documentVerificationProvider,Check document upload for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:39.024000,onboarding-server,identityVerification,identityVerification,Switched to PRESENCE_CHECK/NOT_INITIALIZED; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:36.085000,onboarding-server,identityVerification,identityVerification,Switched to CLIENT_EVALUATION/ACCEPTED; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:36.072000,onboarding-server,onboardingProvider,onboardingProvider,Client evaluated for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:33.046000,onboarding-server,documentVerification,documentVerification,Document accepted at final verification for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""6def8ed0-c5a7-41f9-ac1e-fb21da117b36"",""documentVerificationId"":""52612076-ae6d-44d1-ae0d-97be2615afd0""}"
2026-03-19 11:53:33.046000,onboarding-server,identityVerification,identityVerification,Switched to DOCUMENT_VERIFICATION_FINAL/ACCEPTED; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:33.046000,onboarding-server,documentVerification,documentVerification,Document accepted at final verification for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""4bce7301-c5b4-446c-9ab8-bf081a342975"",""documentVerificationId"":""52612076-ae6d-44d1-ae0d-97be2615afd0""}"
2026-03-19 11:53:33.046000,onboarding-server,documentVerification,documentVerification,Document accepted at final verification for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""220c7986-300e-4ade-b4eb-6a2c7e45730a"",""documentVerificationId"":""52612076-ae6d-44d1-ae0d-97be2615afd0""}"
2026-03-19 11:53:33.044000,onboarding-server,documentVerificationProvider,documentVerificationProvider,Cross verified documents: ACCEPTED for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:30.029000,onboarding-server,identityVerification,identityVerification,Switched to DOCUMENT_VERIFICATION_FINAL/IN_PROGRESS; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:27.052000,onboarding-server,identityVerification,identityVerification,Switched to DOCUMENT_VERIFICATION/ACCEPTED; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:27.039000,onboarding-server,documentVerification,documentVerification,Document verification status changed to ACCEPTED for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""4bce7301-c5b4-446c-9ab8-bf081a342975""}"
2026-03-19 11:53:27.039000,onboarding-server,documentVerificationProvider,documentVerificationProvider,Documents verified: ACCEPTED for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:24.016000,onboarding-server,identityVerification,identityVerification,Switched to DOCUMENT_UPLOAD/VERIFICATION_PENDING; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:23.406000,onboarding-server,documentVerificationProvider,documentVerificationProvider,"Submit documents for user: mockuser_567780649667363606, document IDs: [4bce7301-c5b4-446c-9ab8-bf081a342975]","{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:23.406000,onboarding-server,documentVerification,documentVerification,Document verification pending for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""4bce7301-c5b4-446c-9ab8-bf081a342975""}"
2026-03-19 11:53:23.384000,onboarding-server,onboardingProvider,onboardingProvider,"Document verification response, user: mockuser_567780649667363606, provider: Microblink, documentType: DRIVING_LICENSE","{""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentResponseJson"":""{\""processingStatus\"":\""Completed\"",\""verification\"":{\""certaintyLevel\"":\""High\"",\""recommendedOutcome\"":\""Accept\"",\""type\"":\""DetailedCheck\"",\""result\"":\""Pass\"",\""performedChecks\"":10},\""checks\"":[{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""ExtractedDataCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""MatchCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""field\"":\""FirstName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""LastName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""FullName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Address\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PlaceOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Race\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Religion\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Profession\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""MaritalStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ResidentialStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Employer\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfIssue\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfExpiry\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentOptionalAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""LogicCheck\"",\""result\"":\""Pass\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""DateLogicCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""DateOfBirthBeforeDateOfIssueCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfBirthBeforeDateOfExpiryCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfIssueBeforeDateOfExpiryCheck\"",\""result\"":\""Fail\""},{\""type\"":\""Check\"",\""name\"":\""DateOfBirthInPastCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfIssueInPastCheck\"",\""result\"":\""Fail\""}]},{\""type\"":\""Check\"",\""name\"":\""DocumentNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""PersonalIdNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""InventoryControlNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""DocumentDiscriminatorLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""CustomerIdNumberLogic\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""FieldFormatCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""field\"":\""DateOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfExpiry\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfIssue\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentOptionalAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""AdditionalPersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Sex\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Nationality\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""IssuingAuthority\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""MaritalStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Religion\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ClassEffectiveDate\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ClassExpiryDate\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""}]},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""BarcodeAuthenticity\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""ContentCheck\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""ReadCheck\"",\""result\"":\""NotPerformed\""}]},{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SuspiciousDataCheck\"",\""result\"":\""Pass\"",\""checks\"":[{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SuspiciousNumberCheck\"",\""result\"":\""Pass\""},{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SampleStringCheck\"",\""result\"":\""Pass\""}]},{\""type\"":\""Check\"",\""name\"":\""DataIntegrityCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""MRZCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""Parsed\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""CheckDigits\"",\""result\"":\""NotPerformed\""}]}]},{\""type\"":\""Check\"",\""name\"":\""DocumentLivenessCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""ScreenCheck\"",\""result\"":\""NotPerformed\""},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""PhotocopyCheck\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""VisualCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""PhotoForgeryCheck\"",\""result\"":\""NotPerformed\""},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""SecurityFeatures\"",\""result\"":\""NotPerformed\"",\""details\"":{}}]},{\""type\"":\""Check\"",\""name\"":\""DocumentValidityCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""VersionCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""ExpiredCheck\"",\""result\"":\""Fail\""}]}],\""processIndicators\"":[{\""name\"":\""Clarity\"",\""type\"":\""ImageQuality\"",\""result\"":\""Pass\""},{\""name\"":\""HandPresence\"",\""type\"":\""ScanProcess\"",\""result\"":\""Fail\""},{\""name\"":\""Cropped\"",\""type\"":\""ImageQuality\"",\""result\"":\""Pass\""}],\""messages\"":[{\""code\"":\""I001\"",\""message\"":\""We've extracted and completed all feasible actions. If the result is inadequate, review the process indicators.\"",\""status\"":\""Info\""}],\""runtime\"":{\""startedOn\"":\""2026-02-01T19:13:58.2391777Z\"",\""finishedOn\"":\""2026-02-01T19:13:59.4174137Z\"",\""elapsedMs\"":1178,\""serviceVersion\"":\""3.17.2\"",\""runnerVersion\"":\""unknown\"",\""runnerInstanceKey\"":\""unknown\"",\""runnerInstanceIndex\"":0,\""wrapperVersion\"":\""3.18.0-amd64\"",\""extractionRecognizerVersion\"":\""17.0.6\"",\""verificationRecognizerVersion\"":\""16.0.1\"",\""clientSdkName\"":\""\"",\""clientSdkVersion\"":\""\"",\""traceId\"":\""00-1f9f307f56322e931e9ebbc771108236-60606cd7b9a2337b-01\""},\""optionsUsed\"":{\""screenMatchLevel\"":\""Disabled\"",\""photocopyMatchLevel\"":\""Disabled\"",\""barcodeAnomalyMatchLevel\"":\""Disabled\"",\""photoForgeryMatchLevel\"":\""Disabled\"",\""staticSecurityFeaturesMatchLevel\"":\""Disabled\"",\""dataMatchMatchLevel\"":\""Disabled\"",\""blurMatchLevel\"":\""Disabled\"",\""glareMatchLevel\"":\""Disabled\"",\""lightingMatchLevel\"":\""Disabled\"",\""sharpnessMatchLevel\"":\""Disabled\"",\""handOcclusionMatchLevel\"":\""Disabled\"",\""dpiMatchLevel\"":\""Disabled\"",\""tiltMatchLevel\"":\""Disabled\"",\""imageQualityInterpretation\"":\""Conservative\"",\""sideMode\"":\""MultiSide\"",\""treatExpirationAsFraud\"":false},\""useCaseUsed\"":{\""documentVerificationPolicy\"":\""Standard\"",\""verificationContext\"":\""InPerson\"",\""manualReviewStrategy\"":\""Never\"",\""manualReviewSensitivity\"":\""Default\"",\""captureConditions\"":\""Basic\""}}""}"
2026-03-19 11:53:18.071000,onboarding-server,identityVerification,identityVerification,Switched to DOCUMENT_UPLOAD/IN_PROGRESS; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:18.062000,onboarding-server,documentVerification,documentVerification,Document verification status changed to ACCEPTED for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""220c7986-300e-4ade-b4eb-6a2c7e45730a""}"
2026-03-19 11:53:18.059000,onboarding-server,documentVerification,documentVerification,Document verification status changed to ACCEPTED for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""6def8ed0-c5a7-41f9-ac1e-fb21da117b36""}"
2026-03-19 11:53:18.058000,onboarding-server,documentVerificationProvider,documentVerificationProvider,Documents verified: ACCEPTED for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:15.023000,onboarding-server,identityVerification,identityVerification,Switched to DOCUMENT_UPLOAD/VERIFICATION_PENDING; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:14.061000,onboarding-server,documentVerificationProvider,documentVerificationProvider,"Submit documents for user: mockuser_567780649667363606, document IDs: [6def8ed0-c5a7-41f9-ac1e-fb21da117b36, 220c7986-300e-4ade-b4eb-6a2c7e45730a]","{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
2026-03-19 11:53:14.061000,onboarding-server,documentVerification,documentVerification,Document verification pending for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""220c7986-300e-4ade-b4eb-6a2c7e45730a""}"
2026-03-19 11:53:14.061000,onboarding-server,documentVerification,documentVerification,Document verification pending for user: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentId"":""6def8ed0-c5a7-41f9-ac1e-fb21da117b36""}"
2026-03-19 11:53:14.035000,onboarding-server,onboardingProvider,onboardingProvider,"Document verification response, user: mockuser_567780649667363606, provider: Microblink, documentType: ID_CARD","{""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606"",""documentResponseJson"":""{\""processingStatus\"":\""Completed\"",\""verification\"":{\""certaintyLevel\"":\""High\"",\""recommendedOutcome\"":\""Accept\"",\""type\"":\""DetailedCheck\"",\""result\"":\""Pass\"",\""performedChecks\"":10},\""checks\"":[{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""ExtractedDataCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""MatchCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""field\"":\""FirstName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""LastName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""FullName\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Address\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PlaceOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Race\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Religion\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Profession\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""MaritalStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ResidentialStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Employer\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfIssue\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfExpiry\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentOptionalAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""LogicCheck\"",\""result\"":\""Pass\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""DateLogicCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""DateOfBirthBeforeDateOfIssueCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfBirthBeforeDateOfExpiryCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfIssueBeforeDateOfExpiryCheck\"",\""result\"":\""Fail\""},{\""type\"":\""Check\"",\""name\"":\""DateOfBirthInPastCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""DateOfIssueInPastCheck\"",\""result\"":\""Fail\""}]},{\""type\"":\""Check\"",\""name\"":\""DocumentNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""PersonalIdNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""InventoryControlNumberLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""DocumentDiscriminatorLogic\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""CustomerIdNumberLogic\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""FieldFormatCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""field\"":\""DateOfBirth\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfExpiry\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DateOfIssue\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""DocumentOptionalAdditionalNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""PersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""AdditionalPersonalIdNumber\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Sex\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Nationality\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""IssuingAuthority\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""MaritalStatus\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""Religion\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ClassEffectiveDate\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""},{\""field\"":\""ClassExpiryDate\"",\""type\"":\""FieldCheck\"",\""result\"":\""NotPerformed\""}]},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""BarcodeAuthenticity\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""ContentCheck\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""ReadCheck\"",\""result\"":\""NotPerformed\""}]},{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SuspiciousDataCheck\"",\""result\"":\""Pass\"",\""checks\"":[{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SuspiciousNumberCheck\"",\""result\"":\""Pass\""},{\""certaintyLevel\"":\""High\"",\""type\"":\""DetailedCheck\"",\""name\"":\""SampleStringCheck\"",\""result\"":\""Pass\""}]},{\""type\"":\""Check\"",\""name\"":\""DataIntegrityCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""MRZCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""Parsed\"",\""result\"":\""NotPerformed\""},{\""type\"":\""Check\"",\""name\"":\""CheckDigits\"",\""result\"":\""NotPerformed\""}]}]},{\""type\"":\""Check\"",\""name\"":\""DocumentLivenessCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""ScreenCheck\"",\""result\"":\""NotPerformed\""},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""PhotocopyCheck\"",\""result\"":\""NotPerformed\""}]},{\""type\"":\""Check\"",\""name\"":\""VisualCheck\"",\""result\"":\""NotPerformed\"",\""checks\"":[{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""PhotoForgeryCheck\"",\""result\"":\""NotPerformed\""},{\""matchLevel\"":\""Unknown\"",\""type\"":\""TieredCheck\"",\""name\"":\""SecurityFeatures\"",\""result\"":\""NotPerformed\"",\""details\"":{}}]},{\""type\"":\""Check\"",\""name\"":\""DocumentValidityCheck\"",\""result\"":\""Fail\"",\""checks\"":[{\""type\"":\""Check\"",\""name\"":\""VersionCheck\"",\""result\"":\""Pass\""},{\""type\"":\""Check\"",\""name\"":\""ExpiredCheck\"",\""result\"":\""Fail\""}]}],\""processIndicators\"":[{\""name\"":\""Clarity\"",\""type\"":\""ImageQuality\"",\""result\"":\""Pass\""},{\""name\"":\""HandPresence\"",\""type\"":\""ScanProcess\"",\""result\"":\""Fail\""},{\""name\"":\""Cropped\"",\""type\"":\""ImageQuality\"",\""result\"":\""Pass\""}],\""messages\"":[{\""code\"":\""I001\"",\""message\"":\""We've extracted and completed all feasible actions. If the result is inadequate, review the process indicators.\"",\""status\"":\""Info\""}],\""runtime\"":{\""startedOn\"":\""2026-02-01T19:13:58.2391777Z\"",\""finishedOn\"":\""2026-02-01T19:13:59.4174137Z\"",\""elapsedMs\"":1178,\""serviceVersion\"":\""3.17.2\"",\""runnerVersion\"":\""unknown\"",\""runnerInstanceKey\"":\""unknown\"",\""runnerInstanceIndex\"":0,\""wrapperVersion\"":\""3.18.0-amd64\"",\""extractionRecognizerVersion\"":\""17.0.6\"",\""verificationRecognizerVersion\"":\""16.0.1\"",\""clientSdkName\"":\""\"",\""clientSdkVersion\"":\""\"",\""traceId\"":\""00-1f9f307f56322e931e9ebbc771108236-60606cd7b9a2337b-01\""},\""optionsUsed\"":{\""screenMatchLevel\"":\""Disabled\"",\""photocopyMatchLevel\"":\""Disabled\"",\""barcodeAnomalyMatchLevel\"":\""Disabled\"",\""photoForgeryMatchLevel\"":\""Disabled\"",\""staticSecurityFeaturesMatchLevel\"":\""Disabled\"",\""dataMatchMatchLevel\"":\""Disabled\"",\""blurMatchLevel\"":\""Disabled\"",\""glareMatchLevel\"":\""Disabled\"",\""lightingMatchLevel\"":\""Disabled\"",\""sharpnessMatchLevel\"":\""Disabled\"",\""handOcclusionMatchLevel\"":\""Disabled\"",\""dpiMatchLevel\"":\""Disabled\"",\""tiltMatchLevel\"":\""Disabled\"",\""imageQualityInterpretation\"":\""Conservative\"",\""sideMode\"":\""MultiSide\"",\""treatExpirationAsFraud\"":false},\""useCaseUsed\"":{\""documentVerificationPolicy\"":\""Standard\"",\""verificationContext\"":\""InPerson\"",\""manualReviewStrategy\"":\""Never\"",\""manualReviewSensitivity\"":\""Default\"",\""captureConditions\"":\""Basic\""}}""}"
2026-03-19 11:53:13.818000,onboarding-server,identityVerification,identityVerification,Switched to DOCUMENT_UPLOAD/IN_PROGRESS; user ID: mockuser_567780649667363606,"{""identityVerificationId"":""189db1e4-7abe-4309-a983-19e6283e3496"",""processId"":""8d0659cb-9e8c-401a-9493-2284f52c5528"",""activationId"":""6c115802-fad3-474a-afe6-b69215740a99"",""userId"":""mockuser_567780649667363606""}"
```


### Parameters

Here is description of the parameters used in the `audit_log.param` and `audit_log.message` columns:

| Parameter                    | Description                                                                                       |
|------------------------------|---------------------------------------------------------------------------------------------------|
| `identityVerificationId`     | Internal identity verification ID                                                                 |
| `processId`                  | ID of the onboarding process                                                                      |
| `activationId`               | PowerAuth activation ID bind with the onboarding process                                          |
| `targetActivationId`         | Final PowerAuth activation ID set after successful onboarding process                             |
| `userId`                     | ID of user passing the onboarding process                                                         |
| `otpId`                      | ID of the OTP used in the onboarding process step                                                 |
| `documentId`                 | Internal document ID used in the onboarding process verification                                  |
| `documentVerificationId`     | ID of document verification operation                                                             |
| `documentResponseJson`       | JSON response from the document verification provider                                             |
| `providerName`               | Document verification provider name                                                               |
| `documentType`               | Type of document                                                                                  |
| `documentUploadId`           | ID of document upload operation. Each document side has its own ID. TODO: link to possible values |
| `documentVerificationStatus` | Status of document verification. TODO: link to possible values                                    |
| `presenceCheckStatus`        | Result of presence check verification. TODO: link to possible values                              |


### Events

Description of the events logged in the `audit_log` table.


#### Documents are uploaded

The user is expected to submit their identity document for verification. No sensitive data has been evaluated yet — the process is waiting for the user to provide evidence of identity.

MESSAGE: `Switched to DOCUMENT_UPLOAD/IN_PROGRESS; user ID: {userId}`


#### Document submitted to the provider

Document is successfully uploaded and sent to the document verification provider.

MESSAGE: `Submit documents for user: {userId}, document IDs: [{documentUploadIds}]`


#### Document evaluation result received from the provider

The provider has returned its evaluation of the submitted identity document. Note that `documentType` reflects what the user claimed via the mobile client — not what the provider independently determined.

MESSAGE: `Document verification response, user: {userId}, provider: {providerName}, documentType: {documentType}`


#### Document pending for final verification

Document is pending for the final verification against the evaluation returned by the provider. This message is logged for each document type and side.

MESSAGE: `Document verification pending for user: {userId}`


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


#### Document final verification is performed

Final verification of the uploaded documents is performed using the evaluation returned by the provider:
- Documents belong to the same person — cross-check
- The claimed document type matches the one identified by the provider

MESSAGE: `Switched to DOCUMENT_VERIFICATION_FINAL/IN_PROGRESS; user ID: {userId}`


#### Result of the final documents verification

Result of the documents final verification.

MESSAGE: `Cross verified documents: {documentVerificationStatus} for user: {userId}`


#### Result of an individual document in the final verification

Result of individual document verification. See `documentId` parameter to identify the document. This message is logged for each document type and side.
This is related to this [event](#result-of-the-final-documents-verification), which is logged for all documents.

MESSAGE: `Document accepted at final verification for user: {userId}`


#### Documents final verification passed

Document final verification passed and the onboarding process continues.

MESSAGE: `Switched to DOCUMENT_VERIFICATION_FINAL/ACCEPTED; user ID: {userId}`


#### Client evaluation processed

Client evaluation was successfully sent to the [Client evaluation service](./External-Onboarding-Services.md#client-evaluation-service).

MESSAGE: `Client evaluated for user: {userId}`


#### Client evaluation accepted

Client evaluation was accepted.

MESSAGE: `Switched to CLIENT_EVALUATION/ACCEPTED; user ID: {userId}`


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


#### Presence check started

A new presence check session was started with the provider.

MESSAGE: `Presence check started for user: {userId}`


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


#### Onboarding process passed

All identity verification checks have been successfully passed. The user's identity is confirmed.

MESSAGE: `Switched to COMPLETED/ACCEPTED; user ID: {userId}`


#### Onboarding process finished

The onboarding process is finished.

MESSAGE: `Process finished for user: {userId}`