# Operation Extensions

Enrollment server may expose mobile token API `api/auth/token/app`, if `enrollment-server.mtoken.enabled` set to `true`.

## Operation Templates

Behavior of the mobile application may be affected via UI JSON at operation template.

### Post Approval Screen

You may define a screen visible after approval of the operation.
There are three types available: `REVIEW`, `MERCHANT_REDIRECT`, and `GENERIC`.
It is possible to substitute template variables based on operation parameters, e.g. `${variableName}`.

#### Review

Payload of the review post approval screen shows the operation attributes.

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

Payload of the merchant redirect post approval screen contains three attributes.

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

Payload of the generic post approval screen may contain any object.

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
