# Migration from 1.9.x to 1.10.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `1.9.x` to version `1.10.0`.

## REST API

### Platform Validation during Registration for Push Messages

The endpoints `POST /api/push/device/register` and `POST /api/push/device/register/token` now use updated platform `platform` values `apns`, `fcm`, and `hms`.
The original values `ios`, `android`, and `huawei` are still supported, but will be removed in a future release.

### Specification of Environment during Registration for Push Messages

It is now possible to specify APNs environment during device registration in Push Server.
The change is reflected by addition of property `environment` in endpoints `POST /api/push/device/register` and `POST /api/push/device/register/token`.

The allowed values of the `environment` parameter are:
- `development` - development APNs host is used for sending push messages
- `production` - production APNs host is used for sending push messages

For platforms other than APNs the parameter is not used, `null` value is allowed.

## Internal Changes

Operation claim now uses the new `POST /rest/v3/operation/claim` for claiming operations instead of `POST /rest/v3/operation/detail` to separate operation claim action from obtaining operation detail.
