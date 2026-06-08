# Changelog


## 2.2.0


### Changed

- Upgraded Docker base image to `ibm-semeru-runtimes:open-jdk-25.0.3.0-jre-noble` (OpenJDK 25) [(1804)](https://github.com/wultra/enrollment-server/issues/1804)
- Use `StructuredLogging` instead of `StructuredArguments` directly [(1795)](https://github.com/wultra/enrollment-server/issues/1795)
- Migrated to Spring Boot 4 and Jackson 3 [(1775)](https://github.com/wultra/enrollment-server/issues/1775)


### Removed

- Removed `Serializable` from JPA entities [(1425)](https://github.com/wultra/enrollment-server/issues/1425)
