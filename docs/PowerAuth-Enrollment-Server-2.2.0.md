# Migration from 2.1.x to 2.2.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `2.1.x` to version `2.2.0`.

No migration steps nor database changes are required.


## Dependency Updates


### Docker Base Image Upgrade

The Docker base image has been upgraded from `ibm-semeru-runtimes:open-21.0.9_10-jre-noble` (OpenJDK 21) to `ibm-semeru-runtimes:open-jdk-25.0.3.0-jre-noble` (OpenJDK 25).
No action is required.


### Spring Boot 4 and Jackson 3

PowerAuth Enrollment Server has been migrated to Spring Boot 4 and Jackson 3.
