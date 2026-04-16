# Migration from 1.9.x to 2.1.x

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `1.9.x` to version `2.1.0`.
Since version `2.0.0` was never released, there is no version between `1.9.x` and `2.1.0`.


## Database Changes

For convenience, you can use liquibase for your database migration.

For manual changes use SQL scripts:

- [PostgreSQL script](./../sql/postgresql/onboarding/migration_1.10.0_2.1.0.sql)
- [Oracle script](./../sql/oracle/onboarding/migration_1.10.0_2.1.0.sql)


### Onboarding Process Configuration

A new table `es_onboarding_process_configuration` has been added.
Also added a foreign key `process_config_id` to the table `es_onboarding_process`.

You have to insert at least one row into the table `es_onboarding_process_configuration`, and configure property `enrollment-server-onboarding.onboarding-process.default-type` (or ENV `ONBOARDING_PROCESS_DEFAULT_TYPE`) to work as a default process type.


### Onboarding Process

Added new columns to the table `es_onboarding_process`:

- `target_activation_id`
- `consent_accepted`

The `consent_accepted` column uses the following semantics:

- `true` = consent was accepted
- `false` = consent is pending
- `null` = legacy value for records created before this column was introduced; this value is treated as already resolved / not pending

When interpreting migrated data, do not treat `null` as equivalent to `false`. Existing legacy records may keep `null` for backward compatibility.


### Removing columns from `es_document_data` table

Columns `activation_id`, `identity_verification_id` and `filename` are removed. This should not cause any data loss because 
the table was never used in production and all these metadata are stored in `es_document_verification` table and 
linked to `es_document_data` records via `upload_id` column.


### Selfie

A new table `es_selfie` has been added to temporarily store selfie images of identity verification.


### Document Verification

A new column `country` has been added to the table `es_document_verification`.


### Add Column subject_id to audit_log table

Added a new indexed column `subject_id` holding an identifier linking the audit record to an entity it is related to (e.g. user ID for user-related audit records).

<!-- begin box warning -->
The auditing tables may be already updated in your database schema if the database schema is not separated for different PowerAuth applications. In case the column `audit_log.subject_id` and its index `audit_log_subject_id_idx` are already present, you can safely skip this migration step.
<!-- end -->


## REST API Changes


### Onboarding Start

The following changes were made to the onboarding start endpoint:

- Added a new optional request field `processType` to specify which onboarding process type should be used.
- Added a new optional response field `activationCode` to return activation code and a mandatory field `activationType`.


### Removing large file upload endpoint

Endpoint `POST api/identity/document/upload` was removed because it was never used in production.


### Document Submit

The endpoint `/api/identity/document/submit` has been deprecated in favor of `/api/v2/identity/document/submit`.


### Identity Status

The property `config` in `/api/identity/status` response has been deprecated and will be removed in a future release.
Clients should use the dedicated configuration endpoint `/api/configuration` to retrieve `otpResendPeriodSeconds` and other onboarding configuration.


## External Onboarding Services Changes


### Process Type

For external onboarding services, the `processType` field has been added to the request body.


## Configuration


### OTP Configuration

The property `enrollment-server-onboarding.identity-verification.otp.enabled` has been removed in favor of the [database changes](#database-changes), see the table `es_onboarding_process_configuration`.


### Documents configuration

Following configuration properties were removed:

```
enrollment-server-onboarding.document-verification.required.primaryDocuments
enrollment-server-onboarding.document-verification.required.count
```

New logic works in this way:

Field `documents.totalRequiredDocumentsCount` - specifies min count of unique document types required for verification.

Field `documents.groups` - group specifies more rules for set of document types:
- `requiredDocumentsCount` - specifies min count of unique document types required from this group
- `items` - set of documents. Each item specifies document `type`, `sideCount` and optional `country` as ISO 3166-1 alpha-3 code

EXAMPLE:

```json
{
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

For this configuration in total at least 2 unique document types must be submitted for verification. Acceptable combinations:
- `ID_CARD` (2 sides) + `PASSPORT` (1 side)
- `ID_CARD` (2 sides) + `DRIVING_LICENSE` (1 side)
- `PASSPORT` (1 side) + `DRIVING_LICENSE` (1 side)


## Configuration Changes

### Document Verification

Async processing of documents has never been used, so the following configuration properties were removed:

```properties
enrollment-server-onboarding.document-verification.checkInProgressDocumentSubmits.cron=
enrollment-server-onboarding.document-verification.checkDocumentsVerifications.cron=
enrollment-server-onboarding.document-verification.checkDocumentSubmitVerifications.cron=
```


## Cleaning task

Modified the calculation of the retention period for processing personal data (e.g., uploaded documents and selfie photos) as follows. The data are deleted after the expiration time of the process plus the retention period.
Previously, the data was deleted immediately after the retention period, which could lead to the deletion of data for active processes if the process expiration is higher than the data retention.

Records from the following tables are deleted according to this calculation:
- `es_document_data`
- `es_processed_document_data`
- `es_selfie`