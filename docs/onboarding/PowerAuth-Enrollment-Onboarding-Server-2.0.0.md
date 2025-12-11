# Migration from 1.9.x to 2.0.x

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `1.9.x` to version `2.0.0`.


## Database Changes

For convenience, you can use liquibase for your database migration.

For manual changes use SQL scripts:

- [PostgreSQL script](./../sql/postgresql/onboarding/migration_1.10.0_2.0.0.sql)
- [Oracle script](./../sql/oracle/onboarding/migration_1.10.0_2.0.0.sql)


### Onboarding Process Configuration

A new table `es_onboarding_process_configuration` has been added.
Also added a foreign key `process_config_id` to the table `es_onboarding_process`.

You have to insert at least one row into the table `es_onboarding_process_configuration`, and configure property `enrollment-server-onboarding.onboarding-process.default-type` (or ENV `ONBOARDING_PROCESS_DEFAULT_TYPE`) to work as a default process type.

Following configuration properties were removed:

```
enrollment-server-onboarding.document-verification.required.primaryDocuments
enrollment-server-onboarding.document-verification.required.count
```

New logic works in this way:

Field `documents.mandatory` - all document types from this list are mandatory. Verification process won't pass if any of these is missing. Empty by default.

Field `documents.primary` - list of primary document types. Minimum count of provided documents from this list is specified in field `documents.requiredPrimaryDocumentsCount`. Empty by default.

Field `documents.secondary` - list of secondary document types. Complement to primary documents in order to reach `documents.requiredTotalDocumentsCount`. Contains all supported document types by default.

Field `documents.requiredTotalDocumentsCount` - total minimum count of documents required for verification. It is sum of all unique values (document types) from `documents.mandatory`, `documents.primary` and `documents.secondary`. Default is `0`.

Field `documents.requiredPrimaryDocumentsCount` - minimum count of primary documents required for verification. Default is `0`.


## REST API Changes


### Onboarding Start

The following changes were made to the onboarding start endpoint:

- Added a new optional request field `processType` to specify which onboarding process type should be used.
- Added a new optional response field `activationCode` to return activation code and a mandatory field `activationType`.


## External Onboarding Services Changes


### Process Type

For external onboarding services, the `processType` field has been added to the request body.


## Configuration


### OTP Configuration

The property `enrollment-server-onboarding.identity-verification.otp.enabled` has been removed in favor of the [database changes](#database-changes), see the table `es_onboarding_process_configuration`.
