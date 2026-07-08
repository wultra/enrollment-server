# Copilot Instructions — enrollment-server

This file captures conventions used when working with GitHub Copilot in the `enrollment-server` repository and the broader Wultra PowerAuth ecosystem.

---


## Build and Test


### Commands

```bash
# Full build
mvn clean install

# Run all tests (skip external-service tests)
mvn test

# Run a single test class
mvn -Dtest=MyServiceTest test

# Run a single test method
mvn -Dtest=MyServiceTest#myMethod test

# Run tests including external-service (requires real credentials/env)
mvn test -Dgroups=external-service
```


### External-service tests

Tests that hit real third-party APIs are tagged `@Tag("external-service")` and use `@ActiveProfiles("external-service")`. They are excluded from the standard Surefire run. To add one, follow the pattern in `IProovPresenceCheckProviderTest` or `ZenidDocumentVerificationProviderTest`.

---


## Architecture


### Module overview

| Module                                              | Purpose                                                                        |
|-----------------------------------------------------|--------------------------------------------------------------------------------|
| `enrollment-server`                                 | Core enrollment app: PowerAuth API, push registration, mobile token operations |
| `enrollment-server-api-model`                       | DTOs for enrollment-server REST API                                            |
| `enrollment-server-onboarding`                      | Main onboarding WAR app; orchestrates the full KYC flow                        |
| `enrollment-server-onboarding-api`                  | SPIs for document verification and presence check providers                    |
| `enrollment-server-onboarding-api-model`            | DTOs shared across the onboarding API                                          |
| `enrollment-server-onboarding-common`               | Shared domain/service/repository layer (JPA entities, Flyway migrations)       |
| `enrollment-server-onboarding-domain-model`         | Enums and domain value objects                                                 |
| `enrollment-server-onboarding-adapter-mock`         | Mock provider implementations for local development                            |
| `enrollment-server-onboarding-provider-innovatrics` | Innovatrics integration (document verification + presence check)               |
| `enrollment-server-onboarding-provider-iproov`      | iProov integration (presence check)                                            |
| `enrollment-server-onboarding-provider-microblink`  | Microblink integration (document verification)                                 |
| `enrollment-server-onboarding-provider-zenid`       | ZenID integration (document verification)                                      |
| `mtoken-model`                                      | Mobile token model shared with other Wultra projects                           |


### Provider activation

Providers are selected at runtime via application properties. The `enrollment-server-onboarding` app reads:

```properties
enrollment-server-onboarding.document-verification.provider=innovatrics  # or: zenid, microblink, mock
enrollment-server-onboarding.presence-check.provider=innovatrics          # or: iproov, mock
```

Each provider bean uses `@ConditionalOnExpression` to check the active provider name, e.g.:

```java
@ConditionalOnExpression("""
    '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics'
    and '${enrollment-server-onboarding.document-verification.provider}' == 'innovatrics'
    """)
@Component
public class InnovatricsDocumentVerificationProvider implements DocumentVerificationProvider { ... }
```

The three SPIs to implement are `OnboardingProvider`, `DocumentVerificationProvider`, and `PresenceCheckProvider` — all in `enrollment-server-onboarding-api`, all annotated `@PublicSpi`.


### Database and migrations

Liquibase changelogs live under `docs/db/changelog/`. The master file is `docs/db/changelog/db.changelog-master.xml`; changesets for the onboarding module are under `docs/db/changelog/changesets/enrollment-server-onboarding/`. JPA entities live in `enrollment-server-onboarding-common` under `…/common/database/entity/`.


### REST response wrappers

All controllers use Wultra core response types:

- Success with body: `ObjectResponse<T>`
- Success no body: `Response`
- Error: `ErrorResponse` (fields: `status`, `responseObject.code`, `responseObject.message`)

Error responses are handled centrally by `DefaultExceptionHandler` (`@ControllerAdvice`). Map new exception types there — do not handle them in controllers. Standard error codes used: `INVALID_REQUEST`, `ERROR_GENERIC`, `REMOTE_COMMUNICATION_ERROR`.

---


## Changelog

This repository ships **multiple independently versioned products**, so it keeps one
`CHANGELOG.md` per product, all at the **repository root** (kept out of the
portal-published `docs/` path):

- `CHANGELOG.md` — root **index** (Keep a Changelog header + links to each product changelog)
- `CHANGELOG-enrollment-server.md` — Enrollment Server product
- `CHANGELOG-onboarding.md` — Onboarding Server product

Update the changelog of the affected product as part of every PR — before creating the
PR, not after merge. If a change touches both products, update both files.

> The per-release migration notes under `docs/onboarding/PowerAuth-*-X.Y.Z.md` are portal
> documentation, **not** the changelog. Do not put changelog entries there.


### Format

Each product changelog **strictly** follows
[Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) and adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html):

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- New feature description [(#N)](https://github.com/wultra/enrollment-server/issues/N)

### Changed

- Changed behaviour description [(#N)](https://github.com/wultra/enrollment-server/issues/N)

### Fixed

- Bug fix description [(#N)](https://github.com/wultra/enrollment-server/issues/N)

## [1.2.3] - 2025-03-01

### Added

- ...

[unreleased]: https://github.com/wultra/enrollment-server/compare/1.2.3...HEAD
[1.2.3]: https://github.com/wultra/enrollment-server/compare/1.2.2...1.2.3
```

**Change type subsections** (use only those that apply):
- `Added` — new features
- `Changed` — changes in existing functionality
- `Deprecated` — soon-to-be removed features
- `Removed` — removed features
- `Fixed` — bug fixes
- `Security` — security vulnerability fixes

**Rules:**
- The header must mention both [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
- Always add new entries under the `## [Unreleased]` section at the top.
- On release: rename `## [Unreleased]` to `## [x.y.z] - YYYY-MM-DD` (ISO 8601 date), add a fresh empty `## [Unreleased]` above it, update the `[unreleased]` reference link, and add the new version's compare link.
- Versions and sections must be linkable via reference-style links at the bottom of the file.
- Each entry: `- <Description starting with a verb> [(#N)](url)` — link to the **issue**, not the PR.
- Descriptions should be human-readable, not raw commit messages (e.g. "Fixed NPE when application list is empty" not "fix #811: add missing import").
- Products version independently: use per-product prefixed git tags and set the Conventional Commits `scope` to the affected component.


### Non-functional and documentation-only changes

| Documentation change | Changelog entry? |
|---|---|
| New/changed **user-facing** docs (API reference, configuration properties, deployment/migration guide, README feature section) | **Yes** — under `Added` or `Changed` |
| Fixing/clarifying existing public docs (typos, broken links, wording) | **No** (unless it corrects a materially wrong instruction) |
| Internal docs (`docs-private/`, ADRs, contributor notes, code comments, Javadoc) | **No** |
| Work-in-progress / **partial** docs not yet usable | **No** — log once the feature/doc is complete |

Skip the changelog update only for changes with no user-visible impact (e.g. pure
CI/tooling changes).

---


## Code Conventions


### Java / Spring Boot

- **Lombok**: always use `@Getter`, `@Setter`, `@Slf4j`, etc. — never write manual getters/setters or `private static final Logger logger = ...`.
- **Java 25 / Lombok**: root `pom.xml` must declare `annotationProcessorPaths` for Lombok in `maven-compiler-plugin` (required since JDK 23 changed annotation processing policy to `none`).
- **Copyright header**: use the Wultra copyright header in all files (Java, XML, logback configs, etc.):
  ```java
  /*
   * PowerAuth Enrollment Server
   * Copyright (C) <year> Wultra s.r.o.
   *
   * This program is free software: you can redistribute it and/or modify
   * it under the terms of the GNU Affero General Public License as published
   * by the Free Software Foundation, either version 3 of the License, or
   * (at your option) any later version.
   *
   * This program is distributed in the hope that it will be useful,
   * but WITHOUT ANY WARRANTY; without even the implied warranty of
   * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   * GNU Affero General Public License for more details.
   *
   * You should have received a copy of the GNU Affero General Public License
   * along with this program.  If not, see <http://www.gnu.org/licenses/>.
   */
  ```
  For XML files (including logback configs), use the XML comment equivalent:
  ```
  <!--
    ~ PowerAuth Enrollment Server
    ~ Copyright (C) <year> Wultra s.r.o.
    ~
    ~ This program is free software: you can redistribute it and/or modify
    ~ it under the terms of the GNU Affero General Public License as published
    ~ by the Free Software Foundation, either version 3 of the License, or
    ~ (at your option) any later version.
    ~
    ~ This program is distributed in the hope that it will be useful,
    ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
    ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    ~ GNU Affero General Public License for more details.
    ~
    ~ You should have received a copy of the GNU Affero General Public License
    ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.
    -->
  ```
- **`@author` tag**: always add `@author <Full Name>, <email>@wultra.com` to class-level Javadoc in both main and test classes.


### Markdown

- Always format tables.
- Add two empty lines above headers and one below.


### Logging

- Use `StructuredLogging` for structured key-value pairs. Available convenience methods: `action(value)`, `state(value)`, `stateInitiated()`, `stateFailed()`, `stateSucceeded()`, `kv(key, value)`.
- The log message should never be an empty string. Otherwise, it appears in Elastic, but the message is `null`, and we lose even the associated parameter values.
- Dev/test logback configs: use `logging-support` module from `java-core` with `%msg%sa%n` pattern (no literal space before `%sa` — the converter prepends its own leading space).
- Production configs: use `LogstashEncoder` for JSON output.

