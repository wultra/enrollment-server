# Mobile Token API
<!-- template api -->

Enrollment Server provides a RESTful API used by the Mobile Token application.

The generated REST API documentation in deployed Enrollment server, and it includes standard PowerAuth RESTful API:

```
http[s]://[host]:[port]/enrollment-server/swagger-ui.html
```

Following endpoints are published in Enrollment Server RESTful API:

<!-- begin remove -->
## Methods

### Operations API

- `POST` [/api/auth/token/app/operation/list](#get-pending-operations) - List pending Mobile Token operations
- `POST` [/api/auth/token/app/operation/detail](#get-operation-detail) - Get detail of a Mobile Token operation
- `POST` [/api/auth/token/app/operation/detail/claim](#claim-operation) - Claim a Mobile Token operation for a user
- `POST` [/api/auth/token/app/operation/history](#get-history-of-operations) - Get history of Mobile Token operations
- `POST` [/api/auth/token/app/operation/authorize](#confirm-operation) - Confirm a Mobile Token operation
- `POST` [/api/auth/token/app/operation/cancel](#reject-operation) - Reject a Mobile Token operation

### Push Registration API

- `POST` [/api/push/device/register](#register-for-push-messages-signed) - Register a mobile device for push messages with PowerAuth signature
- `POST` [/api/push/device/register/token](#register-for-push-messages-token) - Register a mobile device for push messages with PowerAuth token

### Activation Spawn API

- `POST` [/api/activation/code](#activation-code) - Handle request for activation code in activation spawn

### Message Inbox API

- `POST` [/api/inbox/count](#inbox-count) - Get Inbox message count
- `POST` [/api/inbox/message/list](#inbox-message-list) - Get Inbox message list
- `POST` [/api/inbox/message/detail](#inbox-message-detail) - Get Inbox message detail
- `POST` [/api/inbox/message/read](#inbox-message-read) - Read an Inbox message
- `POST` [/api/inbox/message/read-all](#inbox-message-read-all) - Read all Inbox messages
<!-- end -->

### Error Handling

Enrollment Server uses following format for error response body, accompanied by an appropriate HTTP status code. Besides the HTTP error codes that application server may return regardless of server application (such as 404 when resource is not found or 503 when server is down), following status codes may be returned:

| Status | HTTP Code | Description |
|--------|-----------|-------------|
| OK     | 200       | No issue    |
| ERROR  | 400       | Issue with a request format, or issue of the business logic |
| ERROR  | 401       | Unauthorized |

All error responses that are produced by the Enrollment Server have the following body:

```json

{
  "status": "ERROR",
  "responseObject": {
    "code": "ERROR_GENERIC",
    "message": "An error message"
  }
}
```

- `status` - `OK`, `ERROR`
- `code` - `ERROR_GENERIC`
- `message` - Message that describes certain error.


## Mobile Token API for Operations

Mobile token API provides access to operations.

## Mobile API Error Codes

List of error codes in Mobile Token API:

| Code                         | Description                                                                                                                                                                                                                                                         | HTTP Status Code |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| `INVALID_REQUEST`            | Invalid request sent - missing request object in request                                                                                                                                                                                                            | 400              |
| `INVALID_ACTIVATION`         | Activation is not valid (it is different from configured activation). Return this error in case the activation does not exist, or in case the activation is not allowed to perform the action (for example, user did not allow operation approvals on such device). | 400              |
| `POWERAUTH_AUTH_FAIL`        | PowerAuth authentication failed                                                                                                                                                                                                                                     | 401              |
| `OPERATION_ALREADY_FINISHED` | Operation is already finished                                                                                                                                                                                                                                       | 400              |
| `OPERATION_ALREADY_FAILED`   | Operation is already failed                                                                                                                                                                                                                                         | 400              |
| `OPERATION_ALREADY_CANCELED` | Operation is already canceled                                                                                                                                                                                                                                       | 400              |
| `OPERATION_EXPIRED`          | Operation is expired                                                                                                                                                                                                                                                | 400              |
| `OPERATION_FAILED`           | PowerAuth server operation approval fails.                                                                                                                                                                                                                          | 401              |

## Localization

In order to get a correctly localized response, use the `Accept-Language` HTTP header in your request.

<!-- begin api POST /api/auth/token/app/operation/list -->
### Get Pending Operations

Get the list with all operations that are pending confirmation.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/auth/token/app/operation/list</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
    - `Content-Type: application/json`
    - `Accept-Language: en-US`
    - `X-PowerAuth-Authorization: ...`

```json
{}
```

#### Response 200

```json
{
  "status": "OK",
  "responseObject": [
    {
      "id": "7e0ba60f-bf22-4ff5-b999-2733784e5eaa",
      "name": "authorize_payment",
      "data": "A1*A100CZK*Q238400856/0300**D20190629*NUtility Bill Payment - 05/2019",
      "status": "PENDING",
      "operationCreated": "2018-07-02T14:43:13+0000",
      "operationExpires": "2018-07-02T14:48:17+0000",
      "allowedSignatureType": {
        "type": "2FA",
        "variants": [
          "possession_knowledge",
          "possession_biometry"
        ]
      },
      "formData": {
        "title": "Confirm Payment",
        "message": "Hello,\nplease confirm following payment:",
        "attributes": [
          {
            "type": "ALERT",
            "alertType": "WARNING",
            "id": "operation.warning",
            "label": "Balance alert",
            "title": "Insufficient Balance",
            "message": "You have only $1.00 on your account with number 238400856/0300."
          },
          {
            "type": "HEADING",
            "id": "operation.heading",
            "label": "Utility Payment"
          },
          {
            "type": "AMOUNT",
            "id": "operation.amount",
            "label": "Amount",
            "amount": 2199.40,
            "currency": "CZK",
            "amountFormatted": "2199,40",
            "currencyFormatted": "Kč"
          },
          {
            "type": "AMOUNT_CONVERSION",
            "id": "operation.conversion",
            "label": "Conversion Rate",
            "dynamic": false,
            "sourceAmount": 100.00,
            "sourceCurrency": "USD",
            "sourceAmountFormatted": "100.00",
            "sourceCurrencyFormatted": "$",
            "sourceValueFormatted": "$100.00",
            "targetAmount": 2199.40,
            "targetCurrency": "CZK",
            "targetAmountFormatted": "2199,40",
            "targetCurrencyFormatted": "Kč",
            "targetValueFormatted": "2199,40 Kč"
          },
          {
            "type": "KEY_VALUE",
            "id": "operation.account",
            "label": "To Account",
            "value": "238400856/0300"
          },
          {
            "type": "KEY_VALUE",
            "id": "operation.dueDate",
            "label": "Due Date",
            "value": "Jun 29, 2019"
          },
          {
            "type": "NOTE",
            "id": "operation.note",
            "label": "Note",
            "note": "Utility Bill Payment - 05/2019"
          },
          {
            "type": "IMAGE",
            "id": "operation.image",
            "label": "Payment Check Preview",
            "thumbnailUrl": "https://example.com/thumbnail.png",
            "originalUrl": "https://example.com/image.png"
          },
          {
            "type": "PARTY_INFO",
            "id": "operation.partyInfo",
            "label": "Application",
            "partyInfo": {
              "logoUrl": "https://itesco.cz/img/logo/logo.svg",
              "name": "Tesco",
              "description": "Find out more about Tesco...",
              "websiteUrl": "https://itesco.cz/hello"
            }
          }
        ]
      }
    }
  ]
}
```
<!-- end -->

<!-- begin api POST /api/auth/token/app/operation/detail -->
### Get Operation Detail

Get an operation detail. 

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/auth/token/app/operation/detail</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `Accept-Language: en-US`
  - `X-PowerAuth-Authorization: ...`

```json
{
  "requestObject": {
    "id": "7e0ba60f-bf22-4ff5-b999-2733784e5eaa"
  }
}
```

#### Response 200

```json
{
  "status": "OK",
  "responseObject": {
    "id": "7e0ba60f-bf22-4ff5-b999-2733784e5eaa",
    "name": "authorize_payment",
    "data": "A1*A100CZK*Q238400856/0300**D20190629*NUtility Bill Payment - 05/2019",
    "status": "PENDING",
    "operationCreated": "2018-07-02T14:43:13+0000",
    "operationExpires": "2018-07-02T14:48:17+0000",
    "allowedSignatureType": {
      "type": "2FA",
      "variants": [
        "possession_knowledge",
        "possession_biometry"
      ]
    },
    "formData": {
      "title": "Confirm Payment",
      "message": "Hello,\nplease confirm following payment:",
      "attributes": [
        {
          "type": "ALERT",
          "alertType": "WARNING",
          "id": "operation.warning",
          "label": "Balance alert",
          "title": "Insufficient Balance",
          "message": "You have only $1.00 on your account with number 238400856/0300."
        },
        {
          "type": "HEADING",
          "id": "operation.heading",
          "label": "Utility Payment"
        },
        {
          "type": "AMOUNT",
          "id": "operation.amount",
          "label": "Amount",
          "amount": 2199.40,
          "currency": "CZK",
          "amountFormatted": "2199,40",
          "currencyFormatted": "Kč"
        },
        {
          "type": "AMOUNT_CONVERSION",
          "id": "operation.conversion",
          "label": "Conversion Rate",
          "dynamic": false,
          "sourceAmount": 100.00,
          "sourceCurrency": "USD",
          "sourceAmountFormatted": "100.00",
          "sourceCurrencyFormatted": "$",
          "sourceValueFormatted": "$100.00",
          "targetAmount": 2199.40,
          "targetCurrency": "CZK",
          "targetAmountFormatted": "2199,40",
          "targetCurrencyFormatted": "Kč",
          "targetValueFormatted": "2199,40 Kč"
        },
        {
          "type": "KEY_VALUE",
          "id": "operation.account",
          "label": "To Account",
          "value": "238400856/0300"
        },
        {
          "type": "KEY_VALUE",
          "id": "operation.dueDate",
          "label": "Due Date",
          "value": "Jun 29, 2019"
        },
        {
          "type": "NOTE",
          "id": "operation.note",
          "label": "Note",
          "note": "Utility Bill Payment - 05/2019"
        },
        {
          "type": "IMAGE",
          "id": "operation.image",
          "label": "Payment Check Preview",
          "thumbnailUrl": "https://example.com/thumbnail.png",
          "originalUrl": "https://example.com/image.png"
        },
        {
          "type": "PARTY_INFO",
          "id": "operation.partyInfo",
          "label": "Application",
          "partyInfo": {
            "logoUrl": "https://itesco.cz/img/logo/logo.svg",
            "name": "Tesco",
            "description": "Find out more about Tesco...",
            "websiteUrl": "https://itesco.cz/hello"
          }
        }
      ]
    }
  }
}
```
<!-- end -->

<!-- begin api POST /api/auth/token/app/operation/detail/claim -->
### Claim Operation

Claim an operation for a user.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/auth/token/app/operation/detail/claim</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `Accept-Language: en-US`
  - `X-PowerAuth-Authorization: ...`

```json
{
  "requestObject": {
    "id": "7e0ba60f-bf22-4ff5-b999-2733784e5eaa"
  }
}
```

#### Response 200

```json
{
  "status": "OK",
  "responseObject": {
    "id": "7e0ba60f-bf22-4ff5-b999-2733784e5eaa",
    "name": "authorize_payment",
    "data": "A1*A100CZK*Q238400856/0300**D20190629*NUtility Bill Payment - 05/2019",
    "status": "PENDING",
    "operationCreated": "2018-07-02T14:43:13+0000",
    "operationExpires": "2018-07-02T14:48:17+0000",
    "allowedSignatureType": {
      "type": "2FA",
      "variants": [
        "possession_knowledge",
        "possession_biometry"
      ]
    },
    "formData": {
      "title": "Confirm Payment",
      "message": "Hello,\nplease confirm following payment:",
      "attributes": [
        {
          "type": "ALERT",
          "alertType": "WARNING",
          "id": "operation.warning",
          "label": "Balance alert",
          "title": "Insufficient Balance",
          "message": "You have only $1.00 on your account with number 238400856/0300."
        },
        {
          "type": "HEADING",
          "id": "operation.heading",
          "label": "Utility Payment"
        },
        {
          "type": "AMOUNT",
          "id": "operation.amount",
          "label": "Amount",
          "amount": 2199.40,
          "currency": "CZK",
          "amountFormatted": "2199,40",
          "currencyFormatted": "Kč"
        },
        {
          "type": "AMOUNT_CONVERSION",
          "id": "operation.conversion",
          "label": "Conversion Rate",
          "dynamic": false,
          "sourceAmount": 100.00,
          "sourceCurrency": "USD",
          "sourceAmountFormatted": "100.00",
          "sourceCurrencyFormatted": "$",
          "sourceValueFormatted": "$100.00",
          "targetAmount": 2199.40,
          "targetCurrency": "CZK",
          "targetAmountFormatted": "2199,40",
          "targetCurrencyFormatted": "Kč",
          "targetValueFormatted": "2199,40 Kč"
        },
        {
          "type": "KEY_VALUE",
          "id": "operation.account",
          "label": "To Account",
          "value": "238400856/0300"
        },
        {
          "type": "KEY_VALUE",
          "id": "operation.dueDate",
          "label": "Due Date",
          "value": "Jun 29, 2019"
        },
        {
          "type": "NOTE",
          "id": "operation.note",
          "label": "Note",
          "note": "Utility Bill Payment - 05/2019"
        },
        {
          "type": "IMAGE",
          "id": "operation.image",
          "label": "Payment Check Preview",
          "thumbnailUrl": "https://example.com/thumbnail.png",
          "originalUrl": "https://example.com/image.png"
        },
        {
          "type": "PARTY_INFO",
          "id": "operation.partyInfo",
          "label": "Application",
          "partyInfo": {
            "logoUrl": "https://itesco.cz/img/logo/logo.svg",
            "name": "Tesco",
            "description": "Find out more about Tesco...",
            "websiteUrl": "https://itesco.cz/hello"
          }
        }
      ]
    }
  }
}
```
<!-- end -->

<!-- begin api POST /api/auth/token/app/operation/history -->
### Get History of Operations

Get the list of confirmed or rejected Mobile Token operations. 

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/auth/token/app/operation/history</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `Accept-Language: en-US`
  - `X-PowerAuth-Authorization: ...`

```json
{}
```

#### Response 200

```json
{
  "status": "OK",
  "responseObject": [
    {
      "id": "7e0ba60f-bf22-4ff5-b999-2733784e5eaa",
      "name": "authorize_payment",
      "data": "A1*A100CZK*Q238400856/0300**D20190629*NUtility Bill Payment - 05/2019",
      "status": "APPROVED",
      "operationCreated": "2018-07-02T14:43:13+0000",
      "operationExpires": "2018-07-02T14:48:17+0000",
      "allowedSignatureType": {
        "type": "2FA",
        "variants": [
          "possession_knowledge",
          "possession_biometry"
        ]
      },
      "formData": {
        "title": "Confirm Payment",
        "message": "Hello,\nplease confirm following payment:",
        "attributes": [
          {
            "type": "ALERT",
            "alertType": "WARNING",
            "id": "operation.warning",
            "label": "Balance alert",
            "title": "Insufficient Balance",
            "message": "You have only $1.00 on your account with number 238400856/0300."
          },
          {
            "type": "HEADING",
            "id": "operation.heading",
            "label": "Utility Payment"
          },
          {
            "type": "AMOUNT",
            "id": "operation.amount",
            "label": "Amount",
            "amount": 2199.40,
            "currency": "CZK",
            "amountFormatted": "2199,40",
            "currencyFormatted": "Kč"
          },
          {
            "type": "AMOUNT_CONVERSION",
            "id": "operation.conversion",
            "label": "Conversion Rate",
            "dynamic": false,
            "sourceAmount": 100.00,
            "sourceCurrency": "USD",
            "sourceAmountFormatted": "100.00",
            "sourceCurrencyFormatted": "$",
            "sourceValueFormatted": "$100.00",
            "targetAmount": 2199.40,
            "targetCurrency": "CZK",
            "targetAmountFormatted": "2199,40",
            "targetCurrencyFormatted": "Kč",
            "targetValueFormatted": "2199,40 Kč"
          },
          {
            "type": "KEY_VALUE",
            "id": "operation.account",
            "label": "To Account",
            "value": "238400856/0300"
          },
          {
            "type": "KEY_VALUE",
            "id": "operation.dueDate",
            "label": "Due Date",
            "value": "Jun 29, 2019"
          },
          {
            "type": "NOTE",
            "id": "operation.note",
            "label": "Note",
            "note": "Utility Bill Payment - 05/2019"
          },
          {
            "type": "IMAGE",
            "id": "operation.image",
            "label": "Payment Check Preview",
            "thumbnailUrl": "https://example.com/thumbnail.png",
            "originalUrl": "https://example.com/image.png"
          },
          {
            "type": "PARTY_INFO",
            "id": "operation.partyInfo",
            "label": "Application",
            "partyInfo": {
              "logoUrl": "https://itesco.cz/img/logo/logo.svg",
              "name": "Tesco",
              "description": "Find out more about Tesco...",
              "websiteUrl": "https://itesco.cz/hello"
            }
          }
        ]
      }
    }
  ]
}
```
<!-- end -->

<!-- begin api POST /api/auth/token/app/operation/authorize -->
### Confirm Operation

Confirms an operation with given ID and data. This endpoint requires a signature of a type specified by the operation.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/auth/token/app/operation/authorize</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Authorization: ...`

```json
{
  "requestObject": {
    "id": "3699a9c0-45f0-458d-84bc-5bde7ec384f7",
    "data": "A1*A100CZK*Q238400856\/0300**D20190629*NUtility Bill Payment - 05\/2019"
  }
}
```

#### Response 200

```json
{
  "status": "OK"
}
```
<!-- end -->

<!-- begin api POST /api/auth/token/app/operation/cancel -->
### Reject Operation

Reject an operation with given ID, with a provided reason.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/auth/token/app/operation/cancel</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Authorization: ...`

```json
{
  "requestObject": {
    "id": "352d6cfa-b8d7-4366-af1f-c99b071b4dc4",
    "reason": "INCORRECT_DATA"
  }
}
```

#### Response 200

```json
{
  "status": "OK"
}
```
<!-- end -->

## Enumerations

### Form Attribute Types

| Type                | Description                                                                       |
|---------------------|-----------------------------------------------------------------------------------|
| `AMOUNT`            | Form field representing an amount with currency.                                  |
| `AMOUNT_CONVERSION` | Form field representing a conversion between amounts.                             |
| `KEY_VALUE`         | Form field representing a key value item with single-line value.                  |
| `NOTE`              | Form field representing a generic text note with multi-line value.                |
| `HEADING`           | Form field representing a heading, where label is displayed as the heading text.  |
| `ALERT`             | Form field representing a alert with success, info, warning or error states.      |
| `PARTY_INFO`        | Form field representing a structured object with information about a third party. |
| `IMAGE`             | Form field representing an image.                                                 |

### Operation Rejection Reasons

| Type | Description |
|---|---|
| `UNKNOWN` | User decided not to tell us the operation rejection reason. |
| `INCORRECT_DATA` | User claims incorrect data was presented in mToken app. |
| `UNEXPECTED_OPERATION` | User claims he/she did not expect any operation. |

### Allowed Signature Types

| Type | Description |
|---|---|
| `1FA` | One-factor signature - user just has to tap "Confirm" button to confirm it. |
| `2FA` | Two-factor signature - user needs to use either password of biometry as addition to possession factor. The `variants` key then determines what signature type is allowed for the given operation. |
| `ECDSA` | ECDSA signature with device private key. |


## Mobile Push Registration API

In order to register mobile device to the push notifications, following endpoints are published.

<!-- begin api POST /api/push/device/register -->
### Register for Push Messages (Signed)

Registers a device to push notifications. Authorization is done using PowerAuth signature.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/push/device/register</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Authorization: ...`

```json
{
  "requestObject": {
    "platform": "ios",
    "token": "10de0b9c-791f-4e9f-93c4-e2203951c307"
  }
}
```

Supported platforms:
- `ios`
- `android`

#### Response 200

```json
{
  "status": "OK"
}
```

#### Response 400

```json
{
    "status": "ERROR",
    "responseObject": {
        "code": "PUSH_REGISTRATION_FAILED",
        "message": "Push registration failed in Mobile Token API component."
    }
}
```

Possible error codes:
- `PUSH_REGISTRATION_FAILED` - returned when Push Server returns error during registration.
- `INVALID_REQUEST` - returned when request object is invalid.
- `INVALID_ACTIVATION` - returned when application or activation is invalid.

#### Response 401

Returned when PowerAuth authentication fails.

```json
{
  "status": "ERROR",
  "responseObject": {
    "code": "POWERAUTH_AUTH_FAIL",
    "message": "Unable to verify device registration"
  }
}
```

<!-- end -->

<!-- begin api POST /api/push/device/register/token -->
### Register for Push Messages (Token)

Registers a device to push notifications. Authorization is done using PowerAuth token.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/push/device/register/token</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Token: ...`

```json
{
  "requestObject": {
    "platform": "ios",
    "token": "10de0b9c-791f-4e9f-93c4-e2203951c307"
  }
}
```

Supported platforms:
- `ios`
- `android`

#### Response 200

```json
{
  "status": "OK"
}
```

#### Response 400

```json
{
    "status": "ERROR",
    "responseObject": {
        "code": "PUSH_REGISTRATION_FAILED",
        "message": "Push registration failed in Mobile Token API component."
    }
}
```

Possible error codes:
- `PUSH_REGISTRATION_FAILED` - returned when Push Server returns error during registration.
- `INVALID_REQUEST` - returned when request object is invalid.
- `INVALID_ACTIVATION` - returned when application or activation is invalid.

#### Response 401

Returned when PowerAuth authentication fails.

```json
{
  "status": "ERROR",
  "responseObject": {
    "code": "POWERAUTH_AUTH_FAIL",
    "message": "Unable to verify device registration"
  }
}
```

<!-- end -->

## Activation Spawn

Activation Spawn API contains a single endpoint which is used for requesting the activation code.

<!-- begin api POST /api/activation/code -->
### Activation Code

Handle a request for activation code in activation spawn.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/activation/code</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Authorization: ...`

```json
{
  "requestObject": {
    "applicationId": "app1",
    "otp": "12345678"
  }
}
```

#### Response 200

```json
{
  "status": "OK"
}
```

<!-- end -->

## Message Inbox

Message Inbox API provides endpoints for managing the message inbox.

<!-- begin api POST /api/inbox/count -->
### Inbox Message Count

Get count of unread messages in Inbox.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/inbox/count</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Token: ...`

```json
{}
```

#### Response 200

```json
{
  "status": "OK",
  "responseObject": {
    "countUnread": 10
  }
}
```

<!-- end -->

<!-- begin api POST /api/inbox/message/list -->
### Inbox Message List

Get messages in Inbox.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/inbox/message/list</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Token: ...`

```json
{
  "requestObject": {
    "page": 0,
    "size": 10,
    "onlyUnread": false
  }
}
```

#### Response 200

```json
{
  "status": "OK",
  "responseObject": [
    {
      "id": "ae641389-d37a-4425-bd14-41c29484596f",
      "type": "text",
      "subject": "Example subject",
      "summary": "Example summary",
      "read": false,
      "timestampCreated": "2022-08-25T22:34:58.702+00:00"
    }
  ]
}
```

<!-- end -->

<!-- begin api POST /api/inbox/message/detail -->
### Inbox Message Detail

Get detail of a message in Inbox.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/inbox/message/detail</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Token: ...`

```json
{
  "requestObject": {
    "id": "ae641389-d37a-4425-bd14-41c29484596f"
  }
}
```

#### Response 200

```json
{
  "status": "OK",
  "responseObject": [
    {
      "id": "ae641389-d37a-4425-bd14-41c29484596f",
      "type": "text",
      "subject": "Example subject",
      "summary": "Example summary",
      "body": "Example message body",
      "read": false,
      "timestampCreated": "2022-08-25T22:34:58.702+00:00"
    }
  ]
}
```

<!-- end -->

<!-- begin api POST /api/inbox/message/read -->
### Inbox Message Read

Mark a message in inbox as read.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/inbox/message/read</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Token: ...`

```json
{
  "requestObject": {
    "id": "ae641389-d37a-4425-bd14-41c29484596f"
  }
}
```

#### Response 200

```json
{
  "status": "OK"
}
```

<!-- end -->

<!-- begin api POST /api/inbox/message/read-all -->
### Inbox Message Read All

Mark all messages in inbox as read.

<!-- begin remove -->

<table>
    <tr>
        <td>Method</td>
        <td><code>POST</code></td>
    </tr>
    <tr>
        <td>Resource URI</td>
        <td><code>/api/inbox/message/read-all</code></td>
    </tr>
</table>
<!-- end -->

#### Request

- Headers:
  - `Content-Type: application/json`
  - `X-PowerAuth-Token: ...`

```json
{}
```

#### Response 200

```json
{
  "status": "OK"
}
```

<!-- end -->
