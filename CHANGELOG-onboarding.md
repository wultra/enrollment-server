# Changelog

All notable changes to the **Onboarding Server** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]


## [2.2.4] 2026-09-02


### Fixed

- Published `DOCUMENT_VERIFICATION_FINISHED` events for documents rejected or failed during upload [(#1894)](https://github.com/wultra/enrollment-server/issues/1894)


## [2.2.3] 2026-08-18


### Added

- Added support for identity verification using an existing active activation [(#1852)](https://github.com/wultra/enrollment-server/issues/1852)


### Fixed

- Fixed 500 error on `/api/identity/status` during reKYC when multiple onboarding processes exist for the same activation ID [(#1864)](https://github.com/wultra/enrollment-server/issues/1864)
- Fixed `/api/identity/status` to return the latest identity verification attempt for the latest onboarding process during reKYC [(#1869)](https://github.com/wultra/enrollment-server/issues/1869)


## [2.2.2] - 2026-08-18


### Fixed

- Fix parsing for documents without an expiration date. [(#1875)](https://github.com/wultra/enrollment-server/issues/1875)


## [2.2.0] - 2026-07-21

### Added

- Added Bean Validation for the REST API [(#1466)](https://github.com/wultra/enrollment-server/issues/1466)
- Added validation for `OnboardingProcessConfigurationValue` [(#1571)](https://github.com/wultra/enrollment-server/issues/1571)

### Changed

- Upgraded Docker base image to OpenJDK 25 [(#1804)](https://github.com/wultra/enrollment-server/issues/1804)
- Proceed with identity verification to the next stage once the required number of accepted documents is met, even if some documents failed [(#1783)](https://github.com/wultra/enrollment-server/issues/1783)
- Use `StructuredLogging` instead of `StructuredArguments` directly [(#1795)](https://github.com/wultra/enrollment-server/issues/1795)
- Changed iProov `resource` attribute value [(#1791)](https://github.com/wultra/enrollment-server/issues/1791)
- Publish event types of `DOCUMENT_VERIFICATION_FINISHED`, `FINAL_DOCUMENT_VERIFICATION_FINISHED`, `PRESENCE_CHECK_FINISHED` and `PROCESS_FINISHED` [(#1751)](https://github.com/wultra/enrollment-server/issues/1751)
- Refactored state machine configuration, limited `EVENT_NEXT_STATE` and sped up processing [(#1592)](https://github.com/wultra/enrollment-server/issues/1592)
- Refactored state machine service to improve batch processing and remove `ProcessIdentifierGuard` [(#1619)](https://github.com/wultra/enrollment-server/issues/1619)
- Migrated to Spring Boot 4 and Jackson 3 [(#1775)](https://github.com/wultra/enrollment-server/issues/1775)
- Migrated Spring Retry to Spring Framework Retry [(#1784)](https://github.com/wultra/enrollment-server/issues/1784)
- Changed field for parsing Microblink document extracted data [(#1760)](https://github.com/wultra/enrollment-server/issues/1760)
- Added Microblink use case configuration property [(#1616)](https://github.com/wultra/enrollment-server/issues/1616)
- Updated User Data Store integration to fetch data and call create document [(#1723)](https://github.com/wultra/enrollment-server/issues/1723) [(#1724)](https://github.com/wultra/enrollment-server/issues/1724)
- Made `ProcessEventRequest.externalUserId` nullable [(#1803)](https://github.com/wultra/enrollment-server/issues/1803)
- Improved structured log quality [(#1773)](https://github.com/wultra/enrollment-server/issues/1773)
- Consolidated changelogs to strict Keep a Changelog 1.1.0 [(#1811)](https://github.com/wultra/enrollment-server/issues/1811)

### Removed

- Removed `Serializable` from JPA entities [(#1425)](https://github.com/wultra/enrollment-server/issues/1425)
- Removed property `enrollment-server-onboarding.identity-verification.enabled` [(#1788)](https://github.com/wultra/enrollment-server/issues/1788)

### Fixed

- Include `id` and `timestamp` in the outgoing process event request [(#1751)](https://github.com/wultra/enrollment-server/issues/1751)
- Fixed issues found by Coverity scan [(#1832)](https://github.com/wultra/enrollment-server/issues/1832)
- Fixed Microblink null pointer dereferences [(#1718)](https://github.com/wultra/enrollment-server/issues/1718)
- Fixed process failing for specific ESO configuration [(#1730)](https://github.com/wultra/enrollment-server/issues/1730)
- Fixed event publishing after onboarding process success is committed [(#1725)](https://github.com/wultra/enrollment-server/issues/1725)
- Removed duplicated `es_document_data` index [(#1752)](https://github.com/wultra/enrollment-server/issues/1752)
- Fixed `onFail=MARK_RAN` in Liquibase preconditions for processed document data table [(#1786)](https://github.com/wultra/enrollment-server/issues/1786)
- Fixed `Hash#sha256` throwing `CryptoProviderException` [(#1756)](https://github.com/wultra/enrollment-server/issues/1756)
- Configured annotationProcessorPaths for Lombok in maven-compiler-plugin [(#1781)](https://github.com/wultra/enrollment-server/issues/1781)
- Populated message field for structured (kv) log calls [(#1819)](https://github.com/wultra/enrollment-server/issues/1819)

[unreleased]: https://github.com/wultra/enrollment-server/compare/2.2.4...HEAD
[2.2.4]: https://github.com/wultra/enrollment-server/compare/2.2.3...2.2.4
[2.2.3]: https://github.com/wultra/enrollment-server/compare/2.2.2...2.2.3
[2.2.2]: https://github.com/wultra/enrollment-server/compare/2.2.0...2.2.2
[2.2.0]: https://github.com/wultra/enrollment-server/compare/2.1.1...2.2.0
