# Enrollment Server

Base implementation of the enrollment server, usable as the stub for project bootstrapping. The enrollment server publishes the following services:

- PowerAuth Standard RESTful API
- Push Registration API
- Mobile Token Operations API

## Document Verification
The document verification process is currently supported for following providers:
- [ZenID](https://zenid.trask.cz/)
- Mock - useful for simple testing and local runs

### ZenID

#### Configuration - API key
The authorization of all API calls is secured by an API key value. It has to be sent as the `Authorization: api_key VALUE` header value. 
Check the bottom of the `Manual/Configuration` page for more details.

The API key value can be configured/get from the `Access` page configuration:
- Role to be granted: `ApiFull`
- Condition: `ApiKeyEqualsValue`
- Value: the value here is the value of the API key

#### Configuration - Validators
It is recommended to create a custom validation profile.  The sensitivity of selected validators can be tuned-up or disabled completely at the `Sensitivity` page.
The profile can be then set as the default or specified in the configuration properties.

#### SDK Initialization
When calling `document-verification/init-sdk` following implementation fields are used:
- Init token - send a token value `sdk-init-token` in the request body `attributes` map field 
- SDK response - receive the value under `zenid-sdk-init-response` from the response `attributes` map field

pro ZenID:
v request atributech poslat hodnotu v sdk-init-token
přijde pak response pro SDK jako zenid-sdk-init-response

## Presence Check
The document verification process is currently supported for following providers:
- [iProov](https://www.iproov.com/)
- Mock - useful for simple testing and local runs

#### Configuration
There are a few needed configuration changes to bring a successful integration. All the followin configuration tuning
has to be requested from the iProov's [support team](https://iproov.freshdesk.com/support/login) on a per-service basis:
- presence check image
  - an accepted person image from finished and successful presence check process
  - requires enabling of the `frame` response feature for [/api/v2/claim/verify/validate](https://secure.iproov.me/docs.html#operation/userVerifyValidate)
- failure reason
  - a more detailed description of the presence check failure
  - requires enabling of the `reason` response feature for [/api/v2/claim/verify/validate](https://secure.iproov.me/docs.html#operation/userVerifyValidate)
