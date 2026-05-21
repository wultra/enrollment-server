# Migration from 2.1.x to 2.2.0

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `2.1.x` to version `2.2.0`.

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

No database changes are required.


## Configuration


### Events

A new property `enrollment-server-onboarding.onboarding-process.process-event.types` has been added to the configuration.
It contains a list of event types that are supported to be published.
The default value is `PROCESS_FINISHED`, `DOCUMENT_VERIFICATION_FINISHED`, `FINAL_DOCUMENT_VERIFICATION_FINISHED`, and `PRESENCE_CHECK_FINISHED`.