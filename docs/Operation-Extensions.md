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
| `preApprovalScreens`  | `false`  |         | `F`       | Optional information about screen that should be displayed before the operation approval. |

### Pre-approval Screen

<!-- begin box warning -->
Mind that `preApprovalScreen` is deprecated, use `preApprovalScreens` instead.
See [Pre-approval Screens](#pre-approval-screens) for more details.
<!-- end -->

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


### Pre-approval Screens

Optional information about screens that should be displayed before the operation approval.

#### Example

```json
{
  "preApprovalScreens": [
    {
      "id": "custom_id1",
      "type": "WARNING",
      "backButton": true,
      "image": "image-label",
      "heading": "Watch out!",
      "message": "You may become a victim of an attack.",
      "elements": [
        {
          "id": "custom_element_id",
          "type": "ALERT",
          "style": "INFO",
          "text": "Make sure the activation takes place on your device"
        },
        {
          "id": "custom_element_id",
          "type": "BUTTON",
          "action": "PHONE",
          "text": "Call center",
          "href": "+42012345678"
        },
        {
          "id": "custom_element_id",
          "type": "LIST_ITEM",
          "icon": "icon-label",
          "text": "You activate a new app and allow access to your accounts"
        }
      ],
      "controls": {
        "flip": true,
        "axis": "VERTICAL",
        "decline": {
          "type": "REJECT",
          "text": "Reject Payment"
        },
        "approve": {
          "type": "BUTTON",
          "text": "Approve Payment",
          "counter": 10
        }
      }
    },
    {
      "id": "custom_id2",
      "type": "QR_SCAN",
      "heading": "Watch out!",
      "message": "You may become a victim of an attack."
    }
  ]
}
```

#### Attributes

| Attribute                                       | Required | Default | Description                                                                                                                                                                       |
|-------------------------------------------------|----------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `preApprovalScreens[].id`                       | `false`  | -       | Custom identifier of the screen.                                                                                                                                                  |
| `preApprovalScreens[].type`                     | `true`   | -       | Type of the screen. (`WARNING`, `INFO`, or `QR_SCAN`)                                                                                                                             |
| `preApprovalScreens[].backButton`               | `false`  | `false` | Whether to display back button on the screen.                                                                                                                                     |
| `preApprovalScreens[].image`                    | `false`  | -       | Image identifier to display on the screen.                                                                                                                                        |
| `preApprovalScreens[].heading`                  | `true`   | -       | Heading of the screen.                                                                                                                                                            |
| `preApprovalScreens[].message`                  | `true`   | -       | Message displayed to the user, placed under the screen heading.                                                                                                                   |
| `preApprovalScreens[].elements`                 | `false`  | `null`  | Array of screen elements (alert, button, list item).                                                                                                                              |
| `preApprovalScreens[].elements[].id`            | `false`  | -       | Custom identifier of the element.                                                                                                                                                 |
| `preApprovalScreens[].elements[].type`          | `true`   | -       | Type of element (`ALERT`, `BUTTON`, `LIST_ITEM`).                                                                                                                                 |
| `preApprovalScreens[].elements[].style`         | `false`  | -       | Style of the element. For `Alert` (`INFO`, `WARNING`, `DANGER`) affects both the alert cell styling and the icon. For `ListItem` elements, the style affects only the icon color. |
| `preApprovalScreens[].elements[].text`          | `true`   | -       | Text content of the element.                                                                                                                                                      |
| `preApprovalScreens[].elements[].action`        | `false`  | -       | Action type for buttons (`LINK`, `MAIL`, `PHONE`).                                                                                                                                |
| `preApprovalScreens[].elements[].href`          | `false`  | -       | Link/reference for the action (e.g. phone number).                                                                                                                                |
| `preApprovalScreens[].elements[].icon`          | `false`  | -       | Icon label for list items.                                                                                                                                                        |
| `preApprovalScreens[].controls`                 | `false`  | `null`  | Configuration of approval controls.                                                                                                                                               |
| `preApprovalScreens[].controls.flip`            | `false`  | `false` | Whether to flip approve/reject buttons.                                                                                                                                           |
| `preApprovalScreens[].controls.axis`            | `false`  | -       | Layout axis for buttons (`VERTICAL`, `HORIZONTAL`).                                                                                                                               |
| `preApprovalScreens[].controls.decline.type`    | `false`  | -       | Type of decline (`BACK`, `REJECT`).                                                                                                                                               |
| `preApprovalScreens[].controls.decline.text`    | `false`  | -       | Text for decline button.                                                                                                                                                          |
| `preApprovalScreens[].controls.approve.type`    | `false`  | -       | Type of approve (`SLIDER`, `BUTTON`).                                                                                                                                             |
| `preApprovalScreens[].controls.approve.text`    | `false`  | -       | Text for approve button.                                                                                                                                                          |
| `preApprovalScreens[].controls.approve.counter` | `false`  | -       | Countdown counter in seconds for approve button.                                                                                                                                  |


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
