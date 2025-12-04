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


## REST API Changes


### Onboarding Start

The following changes were made to the onboarding start endpoint:

- Added a new optional request field `processType` to specify which onboarding process type should be used.
- Added a new optional response field `activationCode` to return activation code and a mandatory field `activationType`.


## External Onboarding Services Changes


### Process Type

For external onboarding services, the `processType` field has been added to the request body.
