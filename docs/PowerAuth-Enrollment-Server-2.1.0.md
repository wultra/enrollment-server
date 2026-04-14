# Migration from 2.0.x to 2.1.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `2.0.x` to version `2.1.0`.

## Database Changes

For convenience, you can use liquibase for your database migration.

For manual changes use SQL scripts:

- [PostgreSQL script](./sql/postgresql/enrollment/migration_2.0.0_2.1.0.sql)
- [Oracle script](./sql/oracle/enrollment/migration_2.0.0_2.1.0.sql)

### Add Column subject_id to audit_log table

Added a new indexed column `subject_id` holding an identifier linking the audit record to an entity it is related to (e.g. user ID for user-related audit records).

<!-- begin box warning -->
The auditing tables may be already updated in your database schema if the database schema is not separated for different PowerAuth applications. In case the column `audit_log.subject_id` and its index `audit_log_subject_id_idx` are already present, you can safely skip this migration step.
<!-- end -->

### Configuration of Activation Removal with 1FA

The activation removal endpoint `/pa/v4/activation/remove` can be configured to allow authentication using `POSSESSION` factor (1FA). By default, two-factor authentication is used when removing activations, either `POSSESSION_KNOWLEDGE` or `POSSESSION_BIOMETRY`.

You can enable 1FA for this endpoint by setting the property:

```properties
activation.remove.allow1fa=true
```

The default value is `false`, meaning that 1FA is not allowed.
