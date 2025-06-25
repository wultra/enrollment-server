# Migration from 1.6.x to 1.7.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `1.6.x` to version `1.7.0`.


## REST API


### Register for Push Messages (Token)

The endpoints `POST /api/push/device/register` and `POST /api/push/device/register/token` now strictly validate `platform` against values `ios`, `android`, or `huawei`.
If you use the PowerAuth SDK, you should not be affected.
