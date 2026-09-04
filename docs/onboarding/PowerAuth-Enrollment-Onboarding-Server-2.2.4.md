# Migration from 2.2.x to 2.2.4

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `2.2.x` to version `2.2.4`.


## Database Changes

The database column `es_document_verification.reject_reason` no longer stores the `documentVerificationRejected` constant.
It now stores the rejection reason returned by the document verification provider, or `Other` when the provider omits it.
