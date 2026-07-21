# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.2.0] - 2026-07-21

### Changed

- Upgraded Docker base image to OpenJDK 25 [(#1804)](https://github.com/wultra/enrollment-server/issues/1804)
- Switched Docker images to use Wultra base image [(#1841)](https://github.com/wultra/enrollment-server/issues/1841)
- Use `StructuredLogging` instead of `StructuredArguments` directly [(#1795)](https://github.com/wultra/enrollment-server/issues/1795)
- Improved structured log quality [(#1773)](https://github.com/wultra/enrollment-server/issues/1773)
- Migrated to Spring Boot 4 and Jackson 3 [(#1775)](https://github.com/wultra/enrollment-server/issues/1775)

### Removed

- Removed `Serializable` from JPA entities [(#1425)](https://github.com/wultra/enrollment-server/issues/1425)

### Fixed

- Fixed create-schema SQL scripts missing migration steps [(#1828)](https://github.com/wultra/enrollment-server/issues/1828)
- Fixed `Hash#sha256` throwing `CryptoProviderException` [(#1756)](https://github.com/wultra/enrollment-server/issues/1756)
- Configured annotationProcessorPaths for Lombok in maven-compiler-plugin [(#1781)](https://github.com/wultra/enrollment-server/issues/1781)
- Populated message field for structured (kv) log calls [(#1819)](https://github.com/wultra/enrollment-server/issues/1819)

[unreleased]: https://github.com/wultra/enrollment-server/compare/2.2.0...HEAD
[2.2.0]: https://github.com/wultra/enrollment-server/compare/2.1.1...2.2.0
