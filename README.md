# JobTrackr API

JobTrackr API is a Spring Boot REST API for tracking job applications. It is being built step by step from the JobTrackr domain in [java-foundations](https://github.com/aref9571/java-foundations). At this stage the project contains the job application domain model, its PostgreSQL table managed by Flyway, a Spring Data repository, a service layer with the application use cases, a health check endpoint and a continuous integration build. The job application endpoints are not implemented yet.

## Tech stack

- Java 21
- Spring Boot 4.1.1 (Spring Web MVC, Validation, Actuator, Spring Data JPA)
- PostgreSQL 17 with Flyway migrations
- Maven with the Maven Wrapper
- JUnit 5, Mockito and Testcontainers
- Docker Compose for the local database
- GitHub Actions

## Requirements

- JDK 21
- Docker with Docker Compose

Maven does not need to be installed. The Maven Wrapper downloads the right version.

## Configuration

The application reads the database settings from environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobtrackr` | JDBC URL of the database |
| `DB_USERNAME` | `jobtrackr` | Database user |
| `DB_PASSWORD` | none | Database password, must be set |

Copy `.env.example` to `.env` and choose a password. Docker Compose reads `.env` to create the database user. The `.env` file is ignored by Git.

```bash
cp .env.example .env
```

## Run

Start PostgreSQL:

```bash
docker compose up -d
```

Start the application with the variables from `.env`:

```bash
set -a; . ./.env; set +a
./mvnw spring-boot:run
```

On Windows set the same variables in your shell and use `mvnw.cmd spring-boot:run`.

Flyway creates the schema on startup from `src/main/resources/db/migration`.

The application starts on port 8080. Check that it is running:

```bash
curl http://localhost:8080/actuator/health
```

The response contains `"status":"UP"`.

Stop the database with `docker compose down`. Add `-v` to delete its data volume as well.

## Test

```bash
./mvnw --batch-mode clean verify
```

The repository and application tests start a PostgreSQL 17 container with Testcontainers, so Docker must be running. The service tests use Mockito and run without a database. The same command runs in GitHub Actions on every push.
