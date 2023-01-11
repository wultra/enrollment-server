# Testing guidelines and approaches

## Integration tests on external services

There are prepared basic integration tests on external services. All such tests 
are [tagged](https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering) with `external-service`.
None of these tests is run during a standard build by default. Run maven command with `-Dgroups="external-service"` to include
also all tests on external services.

Following subchapters list needed system variables to be defined before run of the tests.

### iProov

Following system variables need to be defined:
- IPROOV_API_KEY - api key value
- IPROOV_API_SECRET - api secret value
- IPROOV_ASSURANCE_TYPE - assurance type of the claim, accepts `genuine_presence` (default) or `liveness` values
- IPROOV_RISK_PROFILE - optional configuration of risk tolerance for an authentication attempt
- IPROOV_SERVICE_BASE_URL - e.g. `https://secure.iproov.me/api/v2`
- IPROOV_SERVICE_HOSTNAME - hostname value where the service runs, used in the `Host` header, e.g. `secure.iproov.me`

### ZenID

Following system variables need to be defined:
- ZENID_ASYNC_PROCESSING_ENABLED - allows asynchronous processing, accepts `true` or `false` values
- ZENID_NTLM_USERNAME - a username value for the ntlm authentication
- ZENID_NTLM_PASSWORD - a password value for the ntlm authentication
- ZENID_SERVICE_BASE_URL - hostname value where the service runs, used in the `Host` header, e.g. `secure.iproov.me`
