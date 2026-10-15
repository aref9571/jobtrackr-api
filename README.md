# JobTrackr API

JobTrackr API is a Spring Boot REST API for tracking job applications. It is being built step by step from the JobTrackr domain in [java-foundations](https://github.com/aref9571/java-foundations). At this stage the project contains the application setup, a health check endpoint and a continuous integration build. The job application endpoints are not implemented yet.

## Tech stack

- Java 21
- Spring Boot 4.1.1 (Spring Web MVC, Validation, Actuator)
- Maven with the Maven Wrapper
- JUnit 5
- GitHub Actions

## Requirements

- JDK 21

Maven does not need to be installed. The Maven Wrapper downloads the right version.

## Run

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`.

The application starts on port 8080. Check that it is running:

```bash
curl http://localhost:8080/actuator/health
```

The response contains `"status":"UP"`.

## Test

```bash
./mvnw --batch-mode clean verify
```

The same command runs in GitHub Actions on every push.
