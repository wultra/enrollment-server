# Migration from 2.1.x to 2.2.0

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `2.1.x` to version `2.2.0`.


## Onboarding process state machine

Added limit for identity verification records processed by a single scheduled task calling next state. The default limit is `10_000` and can be configured by property `enrollment-server-onboarding.identity-verification.next-state-batch-size`.


## Replaced Event

We replaced old event type `FINISHED` with the new event type `PROCESS_FINISHED`. Event is triggered at the end of the onboarding process.

The previous event was connected with the end of the identity verification stage. Currently, it is connected with the end of the process. In most cases, there is no difference except for temporary activation. Here, we can have a scenario where identity verification is ACCEPTED, but the process ends in a FAILED state for whatever reason.

The event is triggered if the process ends in a `FINISHED` or `FAILED` state (see `eventData.process.status`).

Old format (event `FINISHED`)

```json
{
    "type": "FINISHED",
    "userId": "",
    "processId": "",
    "processType": "",
    "identityVerificationId": "",
    "eventData": {
        "locale": "",
        "clientIPAddress": "",
        "httpUserAgent": "",
        "requestId": "",
        "fdsData": {}
    }
}
```

New format (event `PROCESS_FINISHED`)

```json
{
    "id": "",
    "timestamp": "",
    "type": "PROCESS_FINISHED",
    "userId": "",
    "externalUserId": "",
    "processId": "",
    "processType": "",
    "identityVerificationId": "",
    "eventData": {
        "process": {
            "status": "FINISHED",
            "errorDetail": null,
            "deviceData": {
                "locale": "",
                "ipAddress": "",
                "httpUserAgent": "",
                "fdsData": {}
            }
        }
    }
}
```

All details are described in [Events Documentation](./Events.md).


## Database Changes

For convenience, you can use liquibase for your database migration.

For manual changes use SQL scripts:

- [PostgreSQL script](../sql/postgresql/onboarding/migration_2.1.0_2.2.0.sql)
- [Oracle script](../sql/oracle/onboarding/migration_2.1.0_2.2.0.sql)


### External User ID

A new column `external_user_id` was added to the `es_onboarding_process` table.
The column stores an external user identifier used by the presence check provider (e.g. iProov).
It is `NULL` at the start of the process and set during the presence check phase.


## Configuration


### Removed Property `enrollment-server-onboarding.identity-verification.enabled`

The property `enrollment-server-onboarding.identity-verification.enabled` has been removed.
Identity verification is now always enabled; the previous behavior is equivalent to always setting the property to `true`.
Remove this property from your configuration.


### Events

A new property `enrollment-server-onboarding.onboarding-process.process-event.types` has been added to the configuration.
It contains a list of event types that are supported to be published.
The default value is `PROCESS_FINISHED`, `DOCUMENT_VERIFICATION_FINISHED`, `FINAL_DOCUMENT_VERIFICATION_FINISHED`, and `PRESENCE_CHECK_FINISHED`.


## Dependency Updates


### Docker Base Image Upgrade

The Docker base image has been upgraded from `ibm-semeru-runtimes:open-21.0.9_10-jre-noble` (OpenJDK 21) to `ibm-semeru-runtimes:open-jdk-25.0.3.0-jre-noble` (OpenJDK 25).
No action is required.


### Spring Boot 4 and Jackson 3

PowerAuth Enrollment Onboarding Server has been migrated to Spring Boot 4 and Jackson 3.
