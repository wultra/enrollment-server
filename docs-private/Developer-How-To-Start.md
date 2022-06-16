# Developer - How to Start Guide


## Enrollment Server


### Standalone Run

- Enable maven profile `standalone`
- Use IntelliJ Idea run configuration at `../.run/EnrollmentServerApplication.run.xml`
- Open [http://localhost:8081/enrollment-server/actuator/health](http://localhost:8081/enrollment-server/actuator/health) and you should get `{"status":"UP"}`


## Enrollment Server Onboarding


### Standalone Run

- Enable maven profile `standalone`
- Use IntelliJ Idea run configuration at `../.run/EnrollmentServerOnboardingApplication.run.xml`
- Open [http://localhost:8083/enrollment-server-onboarding/actuator/health](http://localhost:8083/enrollment-server-onboarding/actuator/health) and you should get `{"status":"UP"}`

