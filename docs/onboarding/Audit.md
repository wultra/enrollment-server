# Audit

This feature provides audit logging for the onboarding server in the database.
For more detailed developer documentation see [auditing library documentation](https://github.com/wultra/java-core?tab=readme-ov-file#wultra-auditing-library).


## Database structure

The auditing library uses the following database structure. 

If a value longer than the character limit is passed to a column, it is truncated to the limit.


### Table `audit_log`

Table of audit log entries

| Column              | Description                                                                                 | Characters limit |
|---------------------|---------------------------------------------------------------------------------------------|------------------|
| `audit_log_id`      | The unique identifier for the audit log entry in UUID format                                | 36               |
| `application_name`  | The name of the application                                                                 | 256              |
| `audit_level`       | The level of the audit log entry. See the [Audit levels](#audit-levels) section             | 32               |
| `audit_type`        | Area of business logic producing the log entry. See the [Audit types](#audit-types) section | 256              |
| `timestamp_created` | Time when the audit log entry was created.                                                  | -                |
| `message`           | The message of the audit log entry                                                          | -                |
| `exception_message` | Error message if specified for the log entry                                                | -                |
| `stack_trace`       | Error stack trace if specified for the log entry                                            | -                |
| `param`             | Extra parameters for the log entry (independent of the `message`)                           | -                |
| `calling_class`     | The calling Java class of the audit log entry                                               | 256              |
| `thread_name`       | The thread producing the log entry                                                          | 256              |
| `version`           | Version of the application                                                                  | 256              |
| `build_time`        | The build time of the application                                                           | -                |


### Table `audit_param`

Table for audit log parameters. It contains the same information as `audit_log.param`, split by parameter name into records into this table.
Storing data in this table is disabled by default and is controlled by the `audit.db.table.param.enabled` property.

<!-- begin box warning -->
The column `audit_log.param` does not have a length limit, but the column `audit_param.param_value` is limited to 4,000 characters. 
If a value exceeding this limit is passed, it is truncated to 4,000 characters. The same applies to the `audit_param.param_key` where the limit is 256 characters.
<!-- end -->

| Column               | Description                                                         | Characters limit |
|----------------------|---------------------------------------------------------------------|------------------|
| `audit_log_id`       | The identifier of the audit log entry from `audit_log.audit_log_id` | 36               |
| `timestamp_created`  | Time when the parameter record was created                          | -                |
| `param_name`         | The name of the parameter from `audit_log.param`.                   | 256              |
| `param_value`        | The value of the parameter from `audit_log.param`.                  | 4000             |


## Configuration

The following application properties can be used to configure audit logging:

| Property Name                                    | Default Value        | Description                                                                                         |
|--------------------------------------------------|----------------------|-----------------------------------------------------------------------------------------------------|
| `spring.application.name`                        | `onboarding-server`  | The name of application used to set `application_name` column                                       |
| `audit.level`                                    | `INFO`               | Threshold of log message levels to be persisted. See the [Audit levels](#audit-levels) section.     |
| `audit.event.queue.size`                         | `100000`             | Size of the internal queue for log entries to be written.                                           |
| `audit.db.cleanup.days`                          | `365`                | Data retention period in the database.                                                              |
| `audit.db.table.log.name`                        | `audit_log`          | Name of the database table for audit log entries.                                                   |
| `audit.db.table.param.enabled`                   | `false`              | Whether values from the column `audit_log.param` are parsed and persisted into `audit_param` table. |
| `audit.db.table.param.name`                      | `audit_param`        | Name of the database table for audit log parameters.                                                |
| `spring.jpa.properties.hibernate.default_schema` | _empty_              | The database schema to use.                                                                         |
| `audit.db.batch.size`                            | `1000`               | Batch size for database operations.                                                                 |


## Audit types

A value in the `audit_log.audit_type` column is used to categorize the audit log entry according to the operation scope.
The following values are used:

| Value                          | Description                                                                                                                                                               |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `process`                      | General onboarding process operation                                                                                                                                      |
| `otp`                          | OTP operations                                                                                                                                                            |
| `identityVerification`         | Identity verification operations                                                                                                                                          |
| `activation`                   | Activation operations                                                                                                                                                     |
| `documentVerification`         | Document verification operations                                                                                                                                          |
| `presenceCheckProvider`        | Presence check operations                                                                                                                                                 |
| `documentVerificationProvider` | Document verification provider operations                                                                                                                                 |
| `onboardingProvider`           | [Onboarding provider](https://developers.wultra.com/components/enrollment-server/develop/documentation/onboarding/External-Onboarding-Services#api-endpoints) operations  |


## Audit levels

A value in the `audit_log.audit_level` column is used to categorize the audit log entry according to severity.

Values from the table below are supported. The table is sorted from the lowest logging level to the highest. Selecting a log level 
includes all levels that precede it. For example, if the level `INFO` is configured, all messages with levels `ERROR`, `WARN` and `INFO` are persisted, 
while messages with levels `DEBUG` and `TRACE` are ignored and not persisted.

| Value    | Description                                               |
|----------|-----------------------------------------------------------|
| `NONE`   | No log message ire persisted - audit logging is disabled. |
| `ERROR`  | Error level                                               |
| `WARN`   | Warning level                                             |
| `INFO`   | Information level                                         |
| `DEBUG`  | Debug level                                               |
| `TRACE`  | Trace level                                               |
| `ALL`    | All levels - same effect as `TRACE`                       |


## How to search in audit logs

The most frequently logged parameters are `activationId` and `userId`. Values are stored either in the `message` or `param` column of the `audit_log` table.

For example, to find all entries with activation ID `6c115802-fad3-474a-afe6-b69215740a99` following query can be used:

```sql
SELECT *
FROM audit_log
WHERE message LIKE '%6c115802-fad3-474a-afe6-b69215740a99%'
OR param LIKE '%6c115802-fad3-474a-afe6-b69215740a99%'
```