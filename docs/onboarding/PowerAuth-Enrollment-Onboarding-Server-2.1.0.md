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


### Add column timestamp_personal_data_cleaned to es_onboarding_process table

The `timestamp_personal_data_cleaned` column track when the personal data associated with an onboarding process has been cleaned up.


### Add column document_verification_id to es_document_data table

The `document_verification_id` column is used to link record with the `es_document_verification`. There is a one-to-one relationship and there is foreign key constraint.


### Onboarding process identity data retention

The identity data retention period is configured using the new property `enrollment-server-onboarding.onboarding-process.completedProcessDataRetentionTime`.
The retention period is measured from the process completion time—either the `timestamp_finished` or `timestamp_failed` column in the `es_onboarding_process` table.
After this period, records linked to the process are deleted from the following tables:
- `es_document_data`
- `es_processed_document_data`
- `es_selfie`
