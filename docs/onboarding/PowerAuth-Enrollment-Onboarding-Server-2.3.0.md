# Migration from 2.2.x to 2.3.0

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `2.2.x` to version `2.3.0`.


## Configuration


### Microblink Required Checks

A new property `enrollment-server-onboarding.document-verification.microblink.requiredChecks` has been added to the configuration.
When set, it lists the 2nd level check names (from the Microblink response `checks` tree) that must all have `result=Pass`,
in addition to `verification.recommendedOutcome=Accept`, for the document to be considered accepted.

The property is optional and empty by default, so existing deployments are unaffected unless the property is explicitly configured.

See [Configuration Properties](./Configuration-Properties.md) and [Configuration of Verification Providers](./Configuration-Verification-Providers.md) for details.
