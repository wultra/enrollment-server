# Customizing Operation Form Data

When creating an operation, you can customize the operation form data. This customization has an effect on how the operation form data is displayed in the Mobile Token application.

## Standard Operation Form Data Attributes

Following attributes are required to be specified for each operation:
- `title` - title of the operation
- `greeting` - message for the user related to the operation displayed in web interface
- `summary` - summary of the operation shown in push messages

## Custom Operation Form Data Attributes

Following structured custom form data attributes are available:
- `AMOUNT` - amount with currency
- `NOTE` - free text
- `BANK_ACCOUNT_CHOICE` - choice of a bank account
- `KEY_VALUE` - generic key-value field
- `BANNER` - banner which is displayed above form field
- `HEADING` - heading with label 
- `PARTY_INFO` - information about party

## Value Formatting

Following form data attributes support value formatting:
- `AMOUNT`
- `NOTE`
- `KEY_VALUE`
- `HEADING`

The value is formatted based on specified format type. The following format types can be used:
- `TEXT` - value is not formatted
- `LOCALIZED_TEXT` - value is localized using localization key from message resources
- `DATE` - value is formatted as date using current locale, expected value format is YYYY-MM-DD
- `NUMBER` - value is formatted as number using current locale
- `AMOUNT` - value is formatted as amount with currency using current locale
- `ACCOUNT` - value is not formatted (reserved for future use)

### Operation Form Data JSON

When creating operations using Next Step API, you can specify operation form data using JSON instead of using the Java API.

Note: `label` and `formattedValue` fields in examples below are always null, because these values are used internally:
- `label` is localized by taking the `id` and localizing it into current language
- `formattedValues` is constructed using logic based on `valueFormatType` and field value

`AMOUNT`:
```json
        {
          "type": "AMOUNT",
          "id": "operation.amount",
          "label": null,
          "valueFormatType": "AMOUNT",
          "formattedValues": {},
          "amount": 100,
          "currency": "CZK",
          "currencyId": "operation.currency"
        }
```

Remarks:
- Always use `AMOUNT` as `valueFormatType`
- The `amount` value can use decimal point
- Use ISO format of `currency`, the value is localized using message resources (e.g. `currency.CZK.name`)
- The `currencyId` value is used to determine localization ID for the word "currency"

`KEY_VALUE`:
```json
        {
          "type": "KEY_VALUE",
          "id": "operation.account",
          "label": null,
          "valueFormatType": "ACCOUNT",
          "formattedValues": {},
          "value": "238400856/0300"
        }
```
Remarks:
- Supported value format types which influence the `formattedValue`: 
  - `TEXT` - non-localized text (as is)
  - `LOCALIZED_TEXT` - localized text (using message resources)
  - `DATE` - date formatted in operation locale
  - `NUMBER` - generic number formatted in operation locale
  - `AMOUNT` - monetary amount formatted in operation locale
  - `ACCOUNT` - account value (not formatted because the syntax may differ greatly)

`NOTE`:
```json
        {
          "type": "NOTE",
          "id": "operation.note",
          "label": null,
          "valueFormatType": "TEXT",
          "formattedValues": {},
          "note": "Utility Bill Payment - 05/2019"
        }
```

Remarks:
- The `note` string is not localized, it is taken "as is".

`HEADING`:
```json
        {
          "type": "HEADING",
          "id": "operation.heading1",
          "label": null,
          "valueFormatType": "TEXT",
          "formattedValues": {},
          "value": "Heading"
        }
```

Remarks:
- The `label` is ignored, the `HEADING` field uses only a value.
- The `value` is formatted using given `valueFormatType` (same value format types as in `KEY_VALUE`).

`BANNER`:
```json
        {
          "type": "BANNER",
          "id": "banner.error",
          "label": null,
          "message": null,
          "bannerType": "BANNER_ERROR"
        }
```

Remarks:
- The `label` is ignored, the `BANNER` field uses only a value.
- The banner message is taken from the `id` field by localizing message resource with such ID.
- Supported banner types:
   - BANNER_ERROR
   - BANNER_WARNING
   - BANNER_INFO

`PARTY_INFO`:
```json
        {
          "type": "PARTY_INFO",
          "id": "operation.partyInfo",
          "label": null,
          "partyInfo": {
            "logoUrl": "https://itesco.cz/img/logo/logo.svg",
            "name": "Tesco",
            "description": "Objevte více příběhů psaných s chutí",
            "websiteUrl": "https://itesco.cz/hello/vse-o-jidle/pribehy-psane-s-chuti/clanek/tomovy-burgery-pro-zapalene-fanousky/15012"
          }
        }
```

Remarks:
- The value is structured and it is not localized.
