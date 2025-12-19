# Migration from 2.0.x to 2.1.x

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `2.0.x` to version `2.1.0`.


## Database Changes

For convenience, you can use liquibase for your database migration.

For manual changes use SQL scripts:

- [PostgreSQL script](./../sql/postgresql/onboarding/migration_2.0.0_2.1.0.sql)
- [Oracle script](./../sql/oracle/onboarding/migration_2.0.0_2.1.0.sql)


### Onboarding Process

Also added a new colum `target_activaiton_id` to the table `es_onboarding_process`.
