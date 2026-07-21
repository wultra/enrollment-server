# Changelog

All notable changes to the **Onboarding Server** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Upgraded Docker base image to `ibm-semeru-runtimes:open-jdk-25.0.3.0-jre-noble` (OpenJDK 25) [(#1804)](https://github.com/wultra/enrollment-server/issues/1804)
- Proceed with identity verification to the next stage once the required number of accepted documents is met, even if some documents failed [(#1783)](https://github.com/wultra/enrollment-server/issues/1783)
- Use `StructuredLogging` instead of `StructuredArguments` directly [(#1795)](https://github.com/wultra/enrollment-server/issues/1795)
- Changed iProov `resource` attribute value [(#1791)](https://github.com/wultra/enrollment-server/issues/1791)
- Publish event types of `DOCUMENT_VERIFICATION_FINISHED`, `FINAL_DOCUMENT_VERIFICATION_FINISHED`, `PRESENCE_CHECK_FINISHED` and `PROCESS_FINISHED` [(#1751)](https://github.com/wultra/enrollment-server/issues/1751)
- Refactored state machine configuration, limited `EVENT_NEXT_STATE` and sped up processing [(#1592)](https://github.com/wultra/enrollment-server/issues/1592)
- Migrated to Spring Boot 4 and Jackson 3 [(#1775)](https://github.com/wultra/enrollment-server/issues/1775)
- Changed field for parsing Microblink document extracted data [(#1760)](https://github.com/wultra/enrollment-server/issues/1760)
- Consolidated changelogs to strict Keep a Changelog 1.1.0 and added Copilot changelog instructions [(#1811)](https://github.com/wultra/enrollment-server/issues/1811)

### Removed

- Removed `Serializable` from JPA entities [(#1425)](https://github.com/wultra/enrollment-server/issues/1425)
- Removed property `enrollment-server-onboarding.identity-verification.enabled` [(#1788)](https://github.com/wultra/enrollment-server/issues/1788)

### Fixed

- Include `id` and `timestamp` in the outgoing process event request [(#1751)](https://github.com/wultra/enrollment-server/issues/1751)

[unreleased]: https://github.com/wultra/enrollment-server/compare/2.1.0...HEAD
