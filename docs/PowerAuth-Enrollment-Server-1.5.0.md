# Migration from 1.4.x to 1.5.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `1.4.x` to version `1.5.0`.


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
