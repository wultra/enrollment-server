# Configuration of Verification Providers

This document describes configuration of providers for personal identity document verification and user presence check.

## Document Verification

The document verification process is currently supported for following providers:
- [ZenID](https://zenid.trask.cz/) - use value `zenid` in configuration
- [Innovatrics](https://www.innovatrics.com/) - use value `innovatrics` in configuration
- [Microblink](https://microblink.com/) - use value `microblink` in configuration
- Mock - useful for simple testing and local runs - use value `mock` in configuration

### ZenID

#### API key

The authorization of all API calls is secured by an API key value. It has to be sent as the `Authorization: api_key VALUE` header value.
Check the bottom of the `Manual/Configuration` page for more details.

The API key value can be configured/get from the `Access` page configuration:
- Role to be granted: `ApiFull`
- Condition: `ApiKeyEqualsValue`
- Value: the value here is the value of the API key

#### Validators

It is recommended to create a custom validation profile. The sensitivity of selected validators can be tuned-up or disabled completely at the `Sensitivity` page.
The profile can be then set as the default or specified in the configuration properties.

#### SDK Initialization

When calling `document-verification/init-sdk` following implementation fields are used:
- Init token - send a token value `sdk-init-token` in the request body `attributes` map field
- SDK response - receive the value under `zenid-sdk-init-response` from the response `attributes` map field

### Innovatrics

Innovatrics documentation for developers can be found at [this link](https://developers.innovatrics.com/digital-onboarding/technical/remote/dot-dis/latest/documentation/).

#### OCR Threshold

During a document validation Innovatrics provides a list of fields extracted from the document, that have OCR
confidence lower than configurable threshold. If the list is not empty, there is a high probability that some
information is read incorrectly. For that reason, this document will be rejected. The OCR confidence threshold is `0.92`
by default, and can be tuned using `innovatrics.dot.dis.customer.document.inspection.ocr-text-field-threshold`.

#### Text Consistency

For each document Innovatrics tries to read visual zone, machine-readable zone and barcode. These isolated parts are
cross-checked during a document validation by Innovatrics. If there are inconsistency between visual zone and
machine-readable zone, or between visual-zone and barcode, the document will be rejected. However, some editions of
identification documents are inconsistent by design. To prevent false rejection of those document modify the
configuration.   
Following example excludes `issuingAuthority` field of Czech identity card 2005 edition from text consistency check:

```yml
innovatrics:
  dot:
    dis:
      customer:
        document:
          inspection:
            text-consistency-check:
              CZE_identity-card_2005-01-01:
                exclusions:
                  - issuingAuthority
```

The format of the document name is `{country}_{type}_{edition}` according to the response of `/metadata` request.


### Microblink

The BlinkID component from Microblink is used for document verification. For details, see [the official Microblink documentation](https://blinkidverify.docs.microblink.com/docs/docver/overview/intro).
It is single REST API endpoint `POST /api/v2/docver` protected by Basic Auth. See [the endpoint specification](https://blinkidverify.docs.microblink.com/docs/docver/api-docs/api-v2).

When a document is uploaded, then it is temporarily stored in cache on Onboarding server. After all documents are uploaded, the endpoint is called for each document.

The mobile SDK uses the Microblink SDK to capture document images. The SDK requires a license key, which is stored in the Onboarding server configuration for each mobile platform.
The license key is sent in the response body of `POST /api/identity/document/init-sdk` as the `license-key` attribute.
The request body should contain following attributes:
 - `platform` - mobile platform for which the key should be returned. Together with `origin` forms a composite key for obtaining `license-key`. Example values: `ios`, `android`, `cordova`.
 - `origin` - Microblink Bundle ID / Application ID. Together with `platform` forms a composite key for obtaining `license-key`.

If any of these attributes are missing or there is no config for given combination then license key is not returned in response body.

Score for client evaluation is calculated based on the value of `certaintyLevel` attribute from the response body.
- `Low` -> 1
- `Medium` -> 5
- `High` -> 10
- `Unknown`, `NotPerformed`, and any other value -> 0


## Presence Check

The document verification process is currently supported for following providers:
- [iProov](https://www.iproov.com/) - use value `iproov` in configuration
- [Innovatrics](https://www.innovatrics.com/) - use value `innovatrics` in configuration
- Mock - useful for simple testing and local runs - use value `mock` in configuration

### iProov

There are a few needed configuration changes to bring a successful integration. All the following configuration tuning
has to be requested from the iProov's [support team](https://iproov.freshdesk.com/support/login) on a per-service basis:
- presence check image
    - an accepted person image from finished and successful presence check process
    - requires enabling of the `frame` response feature for [/api/v2/claim/verify/validate](https://secure.iproov.me/docs.html#operation/userVerifyValidate)
- failure reason
    - a more detailed description of the presence check failure
    - requires enabling of the `reason` response feature for [/api/v2/claim/verify/validate](https://secure.iproov.me/docs.html#operation/userVerifyValidate)
