# Migration from 2.0.x to 2.1.x

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `2.0.x` to version `2.1.0`.

## Database Changes

For convenience, you can use liquibase for your database migration.

For manual changes use SQL scripts:

- [PostgreSQL script](./../sql/postgresql/onboarding/migration_2.0.0_2.1.0.sql)
- [Oracle script](./../sql/oracle/onboarding/migration_2.0.0_2.1.0.sql)

### Add Column subject_id to audit_log table

Added a new indexed column `subject_id` holding an identifier linking the audit record to an entity it is related to (e.g. user ID for user-related audit records).

<!-- begin box warning -->
The auditing tables may be already updated in your database schema if the database schema is not separated for different PowerAuth applications. In case the column `audit_log.subject_id` and its index `audit_log_subject_id_idx` are already present, you can safely skip this migration step.
<!-- end -->


## REST API Changes


### Identity Status

The property `config` in `/api/identity/status` response has been deprecated and will be removed in a future release.
Clients should use the dedicated configuration endpoint `/api/configuration` to retrieve `otpResendPeriodSeconds` and other onboarding configuration.


## Cleaning task

Modified the calculation of the retention period for processing personal data (e.g., uploaded documents and selfie photos) as follows. The data are deleted after the expiration time of the process plus the retention period. 
Previously, the data was deleted immediately after the retention period, which could lead to the deletion of data for active processes if the process expiration is higher than the data retention.

Records from the following tables are deleted according to this calculation:
- `es_document_data`
- `es_processed_document_data`
- `es_selfie`