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


### Onboarding process personal data retention

The cleanup logic was changed and the retention period is configured by `enrollment-server-onboarding.identity-verification.data-retention` and is counted from 
the process completion time (either `timestamp_finished` or `timestamp_failed` column in `es_onboarding_process` table). After this period, records linked to the process 
are deleted from the following tables:
- `es_document_data`
- `es_processed_document_data`
- `es_selfie`

The new cleanup logic uses a new column, `es_onboarding_process.timestamp_personal_data_cleaned`, to track whether personal data has been deleted for a process. 
Because the default value is `NULL`, all processes would be picked up by the first run of cleanup task. 
To reduce the load on the task, it is recommended to set this value for processes that have already been cleaned by the old cleanup logic.
Use the following SQL query to do so:

```sql
UPDATE es_onboarding_process
SET timestamp_personal_data_cleaned = now()
WHERE timestamp_created < now() - interval :cleaned_processes_timestamp;
```

The `:cleaned_processes_timestamp` is calculated as `2 * enrollment-server-onboarding.onboarding-process.expiration + 10 minutes`

For example, if the configuration property `enrollment-server-onboarding.onboarding-process.expiration` is set to `8 hours`, the value of `:cleaned_processes_timestamp` would be `16 hours 10 minutes`.

EXPLANATION OF THE CALCULATION:

Each process expires after `enrollment-server-onboarding.onboarding-process.expiration` from its creation, and no personal data records can be created after that point. Personal data is then deleted 
after the same `enrollment-server-onboarding.onboarding-process.expiration`, calculated from the time the data was created—not from the process creation time.
In the worst case, a personal data record can be created very close to the process expiration time, which is why the configuration property is multiplied by 2. 
The cleanup task for personal data runs every 10 minutes, which accounts for the additional 10 minutes added to the total time.


## Cleaning task

Added a new configuration property `enrollment-server-onboarding.onboarding-process.cleanup-limit` to limit the number of records 
processed in a single run of the task cleaning personal data of completed onboarding processed.