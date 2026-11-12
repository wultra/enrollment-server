# Enrollment Server

Base implementation of the enrollment server, usable as the stub for project bootstrapping. The enrollment server publishes the following services:

- PowerAuth Standard RESTful API
- Push Registration API
- Mobile Token Operations API


## Test coverage

Run `mvn test -Pcoverage` to collect JaCoCo coverage locally. Each module's HTML
report is at `target/site/jacoco/index.html`, alongside `jacoco.xml`. Coverage
also activates automatically in GitHub Actions; a regular local `mvn test` run
does not collect coverage.

The **Test with Maven** workflow publishes line and branch coverage for the
Enrollment Server and its production modules with tests in the run summary.
Totals are calculated from summed covered/total counters, not averaged module
percentages. Metrics with no applicable code are shown as `N/A`; missing or
invalid reports are reported explicitly, and the summary job fails. No minimum
coverage threshold is enforced.

Download the `maven-test-artifacts-<run-id>` artifact from the workflow run to
browse the HTML reports. The test-only mock adapter is not included.
