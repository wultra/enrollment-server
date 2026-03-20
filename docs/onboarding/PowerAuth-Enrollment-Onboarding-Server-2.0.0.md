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


### Onboarding Process

Added a new column `target_activation_id` to the table `es_onboarding_process`.


### Removing columns from `es_document_data` table

Columns `activation_id`, `identity_verification_id` and `filename` are removed. This should not cause any data loss because 
the table was never used in production and all these metadata are stored in `es_document_verification` table and 
linked to `es_document_data` records via `upload_id` column.


### Selfie

A new table `es_selfie` has been added to temporarily store selfie images of identity verification.


## REST API Changes


### Onboarding Start

The following changes were made to the onboarding start endpoint:

- Added a new optional request field `processType` to specify which onboarding process type should be used.
- Added a new optional response field `activationCode` to return activation code and a mandatory field `activationType`.


### Removing large file upload endpoint

Endpoint `POST api/identity/document/upload` was removed because it was never used in production.


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


### Documents data retention

Removed the property `enrollment-server-onboarding.identity-verification.data-retention` (default 1 hour). It controlled how long records were kept 
in the `es_document_data` and `es_processed_document_data` tables based on `timestamp_created`. This setting was independent of process expiration.
The retention time is now controlled by the property `enrollment-server-onboarding.onboarding-process.expiration` (default 3 hours).
