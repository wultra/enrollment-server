# Migration from 1.7.x to 1.8.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `1.7.x` to version `1.8.0`.


## Database Changes

For convenience, you can use liquibase for your database migration.

For manual changes use SQL scripts:

- [PostgreSQL script](./sql/postgresql/enrollment/migration_1.7.0_1.8.0.sql)
- [Oracle script](./sql/oracle/enrollment/migration_1.7.0_1.8.0.sql)


### Add Column result_texts

A column `result_texts` has been added to the table `es_operation_template`.
It is an optional JSON representing customized texts to display for `success`, `failure`, or `reject` operations.
