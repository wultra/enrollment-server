# Migration from 1.10.x to 2.0.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `1.10.x` to version `2.0.0`.


## REST API


### Additional Data for the Operation Rejection

It is now possible to specify `mobileTokenData` attribute at `POST /api/auth/token/app/operation/cancel` request.
The structure is customer-specific.
Could be used, for example, for passing FDS data.


### Deprecation of Pre-approval Screen

The operation UI template attribute `preApprovalScreen` is deprecated, use `preApprovalScreens` instead.


## Java API


### DelegatingActivationCodeHandler

The method `String fetchDestinationApplicationId(String, String, List<String>, List<String>)` in the interface `DelegatingActivationCodeHandler` has been replaced by `TargetApplicationResponse fetchTargetApplication(TargetApplicationRequest);`.
