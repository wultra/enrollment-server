# Changelog

All notable changes to the **Enrollment Server** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Upgraded Docker base image to `ibm-semeru-runtimes:open-jdk-25.0.3.0-jre-noble` (OpenJDK 25) [(#1804)](https://github.com/wultra/enrollment-server/issues/1804)
- Use `StructuredLogging` instead of `StructuredArguments` directly [(#1795)](https://github.com/wultra/enrollment-server/issues/1795)
- Migrated to Spring Boot 4 and Jackson 3 [(#1775)](https://github.com/wultra/enrollment-server/issues/1775)
- Consolidated changelogs to strict Keep a Changelog 1.1.0 and added Copilot changelog instructions [(#1811)](https://github.com/wultra/enrollment-server/issues/1811)

### Removed

- Removed `Serializable` from JPA entities [(#1425)](https://github.com/wultra/enrollment-server/issues/1425)

[unreleased]: https://github.com/wultra/enrollment-server/compare/2.1.0...HEAD
