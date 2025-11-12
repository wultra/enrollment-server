# Migration from 1.9.x to 2.0.x

This guide contains instructions for migration from PowerAuth Enrollment Onboarding Server version `1.9.x` to version `2.0.0`.


## Database Changes


### Onboarding Process Configuration

A new table `es_onboarding_process_configuration` has been added.
Also added a foreign key `process_config_id` to the table `es_onboarding_process`.
