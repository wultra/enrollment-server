# Migration from 2.1.x to 2.2.x

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `2.1.x` to version `2.2.0`.


## Onboarding process state machine

Added limit for identity verification records processed by a single scheduled task calling next state. The default limit is `10_000` and can be configured by property `enrollment-server-onboarding.identity-verification.next-state-batch-size`.