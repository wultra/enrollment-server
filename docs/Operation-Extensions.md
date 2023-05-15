# Operation Extensions

Enrollment server may expose mobile token API `api/auth/token/app`, if `enrollment-server.mtoken.enabled` set to `true`.

## Operation Templates UI Extension

Behavior of the mobile application may be affected via UI JSON at operation template.
Here is an example of complete UI extension.
It will be explained in detail below.

```json
{
  "flipButtons": true,
  "blockApprovalOnCall": false,
  "preApprovalScreen": {
    "type": "WARNING",
    "heading": "Watch out!",
    "message": "You may become a victim of an attack.",
    "items": [
      "You activate a new app and allow access to your accounts",
      "Make sure the activation takes place on your device",
      "If you have been prompted for this operation in connection with a payment, decline it"
    ],
    "approvalType": "SLIDER"
  },
  "postApprovalScreen": {
    "type": "MERCHANT_REDIRECT",
    "heading": "Thank you for your order",
    "message": "You will be redirected to the merchant application.",
    "payload": {
      "redirectText": "Go to the application",
      "redirectUrl": "https://www.example.com",
      "countdown": 5
    }
  }
}
```

### Risk Flags

If you define any UI extension, the operation risk flags are overridden. 

| Attribute             | Required | Default | Risk Flag | Description                                                                               |
|-----------------------|----------|---------|-----------|-------------------------------------------------------------------------------------------|
| `flipButtons`         | `false`  | `false` | `X`       | Flip the approve and reject buttons on the approval screen.                               |
| `blockApprovalOnCall` | `false`  | `false` | `C`       | Block approving the operation in case there is an ongoing call.                           |
| `preApprovalScreen`   | `false`  |         | `F`       | Optional information about screen that should be displayed before the operation approval. |

### Pre-approval Screen

Optional information about screen that should be displayed before the operation approval.

```json
{
  "preApprovalScreen": {
    "type": "WARNING",
    "heading": "Beware of Cyber-Attacks!",
    "message": "This operation is often abused by fraudsters.",
    "items": [
      "You are activation a new mobile app and provide access to your account.",
      "Make sure the activation is happening on the device you own.",
      "If you were asked for this operation over phone, reject it."
    ],
    "approvalType": "SLIDER"
  }
}
```

| Attribute                        | Required | Default | Description                                                                            |
|----------------------------------|----------|---------|----------------------------------------------------------------------------------------|
| `preApprovalScreen.type`         | `true`   | -       | Type of the screen. (`WARNING`, `INFO`, or `QR_SCAN`)                                  |
| `preApprovalScreen.heading`      | `true`   | -       | Heading of the screen.                                                                 |
| `preApprovalScreen.message`      | `true`   | -       | Message displayed to the user, placed under the screen heading.                        |
| `preApprovalScreen.items`        | `false`  | `null`  | Bullet point items displayed by the message.                                           |
| `preApprovalScreen.approvalType` | `false`  | `null`  | Type of the approval screen component. Currently, only a `SLIDER` option is available. |


#### Pre-approval Screen Types

Currently, the following types of pre-approval screen are supported.

- `WARNING` for warning screen.
- `INFO` for a general information screen.
- `QR_SCAN` for screen to scan QR code to do proximity check.


### Post-approval Screen

You may define a screen visible after approval of the operation.
There are three types available: `REVIEW`, `MERCHANT_REDIRECT`, and `GENERIC`.
It is possible to substitute template variables based on operation parameters, e.g. `${variableName}`.

#### Review

Payload of the review post-approval screen shows the operation attributes.

Mind that the payload attributes must be specified again at `es_operation_template.ui`.
It usually could be only subset of `es_operation_template.attributes`.

```json
{
  "postApprovalScreen": {
    "type": "REVIEW",
    "heading": "Successful",
    "message": "The operation was approved.",
    "payload": {
      "attributes": [
        {
          "type": "NOTE",
          "id": "1",
          "label": "test label",
          "note": "${myNote}"
        }
      ]
    }
  }
}
```

#### Merchant Redirect

Payload of the merchant redirect post-approval screen contains three attributes.

- `redirectUrl` - URL to redirect, might be a website or application
- `redirectText` - Label of the redirect URL
- `countdown` - time in seconds before automatic redirect

```json
{
  "postApprovalScreen": {
    "type": "MERCHANT_REDIRECT",
    "heading": "Thank you for your order",
    "message": "You will be redirected to the merchant application.",
    "payload": {
      "redirectText": "Go to the application",
      "redirectUrl": "https://www.example.com",
      "countdown": 5
    }
  }
}
```

#### Generic

Payload of the generic post-approval screen may contain any object.

```json
{
  "postApprovalScreen": {
    "type": "GENERIC",
    "heading": "Thank you for your order",
    "message": "You may close the application now.",
    "payload": {
      "customMessage": "See you next time."
    }
  }
}
```
