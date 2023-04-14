# Migration from 1.4.x to 1.5.x

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `1.4.x` to version `1.5.0`.


## Spring Boot 3

The PowerAuth Enrollment Onboarding Server was upgraded to Spring Boot 3, Spring Framework 6, and Hibernate 6.
It requires Java 17 or newer.

Remove this property.

`spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false`

Make sure that you use dialect without version.

```properties
# spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
# spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
```


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
