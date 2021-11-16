# External providers

## API schema
To update API schema use `https://editor.swagger.io/#/` to get the yaml version
1. File -> Import
2. File -> Save as YAML
3. Update the schema files in the `src/main/resources/api` 

## Presence check providers

### iProov
The [iProov](https://www.iproov.com/) solution can be used for the presence check phase.

There has to be enabled per service feature to get the user's selfie from the verified person check.

[claim validation response](https://secure.iproov.me/docs.html#operation/userVerifyValidate)
- frame_available
```
Present and True if there is frame available for returning to the integrator.

Enabled on a per service provider basis. Contact support@iproov.com to request this functionality.
```
- the jpeg is base64 encoded with escaped slashes (https://stackoverflow.com/questions/1580647/json-why-are-forward-slashes-escaped)
