# Migration from 1.9.x to 1.10.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `1.9.x` to version `1.10.0`.

## REST API

### Platform Validation during Registration for Push Messages

The endpoints `POST /api/push/device/register` and `POST /api/push/device/register/token` now use updated platform `platform` values `apns`, `fcm`, and `hms`.
The original values `ios`, `android`, and `huawei` are still supported, but will be removed in a future release.