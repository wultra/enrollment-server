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

The `document_verification_id` column is used to link a record to the `es_document_verification` table. There is a one-to-one relationship and there is a foreign key constraint.


### Onboarding process identity data retention

The identity data retention period is configured using the property `enrollment-server-onboarding.identity-verification.data-retention`, and the retention period
is measured from the process completion time—either the `timestamp_finished` or `timestamp_failed` column in the `es_onboarding_process` table.

After this period, records linked to the process are deleted from the following tables:
- `es_document_data`
- `es_processed_document_data`
- `es_selfie`

This is a change from the previous version, where the retention period for records was calculated from their `timestamp_created`.
After the upgrade, there may be situations where legacy records in `es_document_data` have a `NULL` value in the `document_verification_id` column—they have not yet been deleted 
by the old cleaning service. Such records will not be deleted by the automatic cleanup task and must be removed manually. Every new record will have document_verification_id set, 
and the cleanup will work as described.

For manual cleanup, check whether any such records exist by executing the following SQL query:

```sql
SELECT count(*)
FROM es_document_data
WHERE document_verification_id IS NULL;
```

If any records exist, they can be deleted using the following SQL query:

```sql
DELETE FROM es_document_data
WHERE document_verification_id IS NULL;
```