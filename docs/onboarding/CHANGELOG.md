# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.2.0] - 2026-07-21

### Changed

- Upgraded Docker base image to OpenJDK 25 [(#1804)](https://github.com/wultra/enrollment-server/issues/1804)
- Proceed with identity verification to the next stage once the required number of accepted documents is met, even if some documents failed [(#1783)](https://github.com/wultra/enrollment-server/issues/1783)
- Use `StructuredLogging` instead of `StructuredArguments` directly [(#1795)](https://github.com/wultra/enrollment-server/issues/1795)
- Change iProov `resource` attribute value [(#1791)](https://github.com/wultra/enrollment-server/issues/1791)
- Publishing event types of `DOCUMENT_VERIFICATION_FINISHED`, `FINAL_DOCUMENT_VERIFICATION_FINISHED`, `PRESENCE_CHECK_FINISHED` and `PROCESS_FINISHED` [(#1751)](https://github.com/wultra/enrollment-server/issues/1751)
- Refactor state machine configuration, limit `EVENT_NEXT_STATE` and speed up processing [(#1592)](https://github.com/wultra/enrollment-server/issues/1592)
- Refactored state machine service to improve batch processing and remove `ProcessIdentifierGuard` [(#1619)](https://github.com/wultra/enrollment-server/issues/1619)
- Migrated to Spring Boot 4 and Jackson 3 [(#1775)](https://github.com/wultra/enrollment-server/issues/1775)
- Migrated Spring Retry to Spring Framework Retry [(#1784)](https://github.com/wultra/enrollment-server/issues/1784)
- Changed field for parsing Microblink document extracted data [(#1760)](https://github.com/wultra/enrollment-server/issues/1760)
- Made `ProcessEventRequest.externalUserId` nullable [(#1803)](https://github.com/wultra/enrollment-server/issues/1803)
- Improved structured log quality [(#1773)](https://github.com/wultra/enrollment-server/issues/1773)

### Removed

- Removed `Serializable` from JPA entities [(#1425)](https://github.com/wultra/enrollment-server/issues/1425)
- Removed property `enrollment-server-onboarding.identity-verification.enabled` [(#1788)](https://github.com/wultra/enrollment-server/issues/1788)

### Fixed

- Include `id` and `timestamp` in the outgoing process event request [(#1751)](https://github.com/wultra/enrollment-server/issues/1751)
- Fixed issues found by Coverity scan [(#1832)](https://github.com/wultra/enrollment-server/issues/1832)
- Populated message field for structured (kv) log calls [(#1819)](https://github.com/wultra/enrollment-server/issues/1819)

[unreleased]: https://github.com/wultra/enrollment-server/compare/2.2.0...HEAD
[2.2.0]: https://github.com/wultra/enrollment-server/compare/2.1.1...2.2.0

