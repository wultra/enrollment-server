# Migration from 1.4.x to 1.5.x

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `1.4.x` to version `1.5.0`.


## Configuration

To avoid confusion, the property `enrollment-server-onboarding.presence-check.iproov.serviceHostname` configuring iProov provider hostname has been dropped and has no effect anymore.
Now, `enrollment-server-onboarding.presence-check.iproov.serviceBaseUrl` is single point to configure iProov URL.


## Database Changes


### Drop MySQL Support

Since version `1.5.0`, MySQL database is not supported anymore.


### FDS Data

A new column `fds_data` has been added to the table `es_onboarding_process`.


#### PostgreSQL

```sql
ALTER TABLE es_onboarding_process
    ADD COLUMN fds_data TEXT;
```


#### Oracle

```sql
ALTER TABLE es_onboarding_process
    ADD fds_data CLOB;
```
