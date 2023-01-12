# Migration from 1.3.x to 1.4.x

This guide contains instructions for migration from PowerAuth Enrollment Server version `1.3.x` to version `1.4.0`.

Since version 1.4.0, there are two war files available.

- `enrollment-server.war` - Original Enrollment server functionality.
- `enrollment-server-onboarding.war` - Onboarding functionality using government-identification and user verifications.

## Onboarding Server

The Onboarding Server has been introduced as a separate war module, see [Documentation for Onboarding Server](./onboarding/Home.md)
and [Onboarding Server Database Structure](./onboarding/Database-Structure.md)
This module is optional. 

### Onboarding Server Configuration

If you want to integrate Onboarding Server, add this dependency to your Enrollment Server.

```xml
<dependency>
    <groupId>com.wultra.security</groupId>
    <artifactId>enrollment-server-onboarding-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

And configuration of the common beans, for example.

```java
@Configuration
@EnableJpaRepositories(basePackages = {
        "com.wultra.app.enrollmentserver.database", // not to override component scan for enrollment-server
        "com.wultra.app.onboardingserver.common.database" // dependencies from enrollment-server-onboarding common
})
@EntityScan(basePackages = {
        "com.wultra.app.enrollmentserver.database.entity", // not to override component scan for enrollment-server
        "com.wultra.app.onboardingserver.common.database.entity" // dependencies from enrollment-server-onboarding common
})
public class OnboardingComponentsConfiguration {

    /**
     * Register onboarding service bean.
     *
     * @param onboardingProcessRepository onboarding process repository
     * @param auditService Audit service.
     * @return onboarding service bean
     */
    @Bean
    public OnboardingService onboardingService(final OnboardingProcessRepository onboardingProcessRepository, final AuditService auditService) {
        return new CommonOnboardingService(onboardingProcessRepository, auditService);
    }

    /**
     * Register otp service bean.
     *
     * @param onboardingOtpRepository onboarding otp repository
     * @param onboardingProcessRepository onboarding process repository
     * @param identityVerificationRepository Identity verification repository.
     * @param onboardingConfig onboarding config
     * @param auditService Audit service.
     * @return otp service bean
     */
    @Bean
    public OtpService otpService(
            final OnboardingOtpRepository onboardingOtpRepository,
            final OnboardingProcessRepository onboardingProcessRepository,
            final IdentityVerificationRepository identityVerificationRepository,
            final CommonOnboardingConfig onboardingConfig,
            final OnboardingProcessLimitService processLimitService,
            final IdentityVerificationLimitService identityVerificationLimitService,
            final AuditService auditService) {

        return new CommonOtpService(onboardingOtpRepository, onboardingProcessRepository, identityVerificationRepository, onboardingConfig, processLimitService, identityVerificationLimitService, auditService);
    }

    /**
     * Register onboarding config bean.
     *
     * @return onboarding config bean
     */
    @Bean
    public CommonOnboardingConfig onboardingConfig() {
        return new CommonOnboardingConfig();
    }

    /**
     * Register process limit service.
     * @param config Common onboarding process configuration.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param auditService Audit service.
     * @return Onboarding process limit service.
     */
    @Bean
    public OnboardingProcessLimitService processLimitService(
            final CommonOnboardingConfig config,
            final OnboardingProcessRepository onboardingProcessRepository,
            final AuditService auditService) {

        return new OnboardingProcessLimitService(config, onboardingProcessRepository, auditService);
    }

    /**
     * Register identity verification limit service.
     * @param identityVerificationRepository Identity verification repository.
     * @param documentVerificationRepository Document verification repository.
     * @param config Onboarding configuration.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param activationFlagService Activation flag service.
     * @param onboardingProcessLimitService Onboarding process limit service.
     * @param auditService Audit service.
     * @return Identity verification limit service.
     */
    @Bean
    public IdentityVerificationLimitService identityVerificationLimitService(
            final IdentityVerificationRepository identityVerificationRepository,
            final DocumentVerificationRepository documentVerificationRepository,
            final CommonOnboardingConfig config,
            final OnboardingProcessRepository onboardingProcessRepository,
            final ActivationFlagService activationFlagService,
            final OnboardingProcessLimitService onboardingProcessLimitService,
            final AuditService auditService) {

        return new IdentityVerificationLimitService(identityVerificationRepository, documentVerificationRepository, config, onboardingProcessRepository, activationFlagService, onboardingProcessLimitService, auditService);
    }

    /**
     * Register activation flag service.
     * @param powerAuthClient PowerAuth client.
     * @param httpCustomizationService HTTP customization service.
     * @return Activation flag service.
     */
    @Bean
    public ActivationFlagService activationFlagService(PowerAuthClient powerAuthClient, HttpCustomizationService httpCustomizationService) {
        return new ActivationFlagService(powerAuthClient, httpCustomizationService);
    }

    /**
     * Register activation otp service bean.
     *
     * @param otpService otp service
     * @return activation otp service bean
     */
    @Bean
    public ActivationOtpService activationOtpService(final OtpService otpService) {
        return new ActivationOtpService(otpService);
    }

    /**
     * Register activation process service bean.
     *
     * @param onboardingService onboading service
     * @return activation process service bean
     */
    @Bean
    public ActivationProcessService activationProcessService(final OnboardingService onboardingService) {
        return new ActivationProcessService(onboardingService);
    }

    /**
     * Register activation exception handler for onboarding.
     * @return Activation exception handler.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ActivationExceptionHandler activationExceptionHandler(){
        return new ActivationExceptionHandler();
    }

    /**
     * Register audit service.
     *
     * @param audit Audit.
     * @return Audit service.
     */
    @Bean
    public AuditService auditService(final Audit audit) {
        return new AuditService(audit);
    }
}
```

## Database Changes

### Operation Template

A new column `ui` has been added to the table `es_operation_template`.

#### PostgreSQL

```sql
ALTER TABLE es_operation_template
    ADD COLUMN ui TEXT;
```

#### Oracle

```sql
ALTER TABLE es_operation_template
    ADD ui CLOB;
```

#### MySQL

```sql
ALTER TABLE es_operation_template
    ADD COLUMN ui TEXT;
```

## Inbox

Inbox functionality is turned on by default if PowerAuth Push Server is running.
It could be turned off by setting property `enrollment-server.inbox.enabled` to `false`.
