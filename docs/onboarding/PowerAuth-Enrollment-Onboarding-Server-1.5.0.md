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

### Total Attempts

A new column `total_attempts` has been added to the table `es_onboarding_otp`.


#### PostgreSQL

```sql
ALTER TABLE es_onboarding_otp
    ADD COLUMN TOTAL_ATTEMPTS INTEGER DEFAULT 0;
```


#### Oracle

```sql
ALTER TABLE es_onboarding_otp
    ADD total_attempts INTEGER DEFAULT 0;
```


### SCA Result

A new table `es_sca_result` has been created.


#### PostgreSQL

```sql
CREATE SEQUENCE es_sca_result_seq INCREMENT BY 50 START WITH 1;

CREATE TABLE es_sca_result
(
    id                       BIGINT      NOT NULL PRIMARY KEY,
    identity_verification_id VARCHAR(36) NOT NULL,
    process_id               VARCHAR(36) NOT NULL,
    presence_check_result    VARCHAR(32),
    otp_verification_result  VARCHAR(32),
    sca_result               VARCHAR(32),
    timestamp_created        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timestamp_last_updated   TIMESTAMP,
    FOREIGN KEY (identity_verification_id) REFERENCES es_identity_verification (id),
    FOREIGN KEY (process_id) REFERENCES es_onboarding_process (id)
);

CREATE INDEX identity_verification_id ON es_sca_result (identity_verification_id);
CREATE INDEX process_id ON es_sca_result (process_id);
```


#### Oracle

```sql
CREATE SEQUENCE ES_SCA_RESULT_SEQ INCREMENT BY 50 START WITH 1;

CREATE TABLE ES_SCA_RESULT
(
    ID                       NUMBER(19)        NOT NULL PRIMARY KEY,
    IDENTITY_VERIFICATION_ID VARCHAR2(36 CHAR) NOT NULL,
    PROCESS_ID               VARCHAR2(36 CHAR) NOT NULL,
    PRESENCE_CHECK_RESULT    VARCHAR2(32 CHAR),
    OTP_VERIFICATION_RESULT  VARCHAR2(32 CHAR),
    SCA_RESULT               VARCHAR2(32 CHAR),
    TIMESTAMP_CREATED        TIMESTAMP(6)      NOT NULL,
    TIMESTAMP_LAST_UPDATED   TIMESTAMP(6),
    FOREIGN KEY (IDENTITY_VERIFICATION_ID) REFERENCES ES_IDENTITY_VERIFICATION (ID),
    FOREIGN KEY (PROCESS_ID) REFERENCES ES_ONBOARDING_PROCESS (ID)
);

CREATE INDEX IDENTITY_VERIFICATION_ID ON ES_SCA_RESULT (IDENTITY_VERIFICATION_ID);
CREATE INDEX PROCESS_ID ON ES_SCA_RESULT (PROCESS_ID);
```


## Dependencies

PostgreSQL JDBC driver is already included in the WAR file.
Oracle JDBC driver remains optional and must be added to your deployment if desired.
