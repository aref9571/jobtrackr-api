# JobTrackr API

JobTrackr API is a Spring Boot REST API for tracking job applications. It is being built step by step from the JobTrackr domain in [java-foundations](https://github.com/aref9571/java-foundations). At this stage the project contains the job application domain model, its PostgreSQL table managed by Flyway, a Spring Data repository, a service layer with the application use cases, REST endpoints for creating, reading, updating, deleting and changing the status of job applications, request validation with consistent error responses, a list endpoint with filters, paging and sorting, a status history for each application, statistics computed in the database, a health check endpoint and a continuous integration build.

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

## API examples

All examples assume the application is running on port 8080. Replace `{id}` with the `id` returned when the application was created.

| Method | Path | Success | Errors |
|---|---|---|---|
| `POST` | `/api/applications` | 201 with a `Location` header | 400, 409 duplicate |
| `GET` | `/api/applications` | 200 with one page of results | 400 |
| `GET` | `/api/applications/{id}` | 200 | 400, 404 |
| `PUT` | `/api/applications/{id}` | 200 | 400, 404, 409 duplicate |
| `PATCH` | `/api/applications/{id}/status` | 200 | 400, 404, 409 status change not allowed |
| `GET` | `/api/applications/{id}/history` | 200 | 400, 404 |
| `GET` | `/api/applications/stats` | 200 | |
| `DELETE` | `/api/applications/{id}` | 204 | 400, 404 |

Create an application:

```bash
curl -i -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{"company":"ACME Corp","role":"Backend Engineer","appliedDate":"2026-10-01","expectedSalaryCents":5000000,"salaryCurrency":"EUR","notes":"Referred by a friend"}'
```

The response is `201 Created` with a `Location` header and the new application:

```json
{"id":"0e85fcf9-d5c2-4e75-8e82-ad8efe50cb4a","company":"ACME Corp","role":"Backend Engineer","status":"APPLIED","appliedDate":"2026-10-01","expectedSalaryCents":5000000,"salaryCurrency":"EUR","notes":"Referred by a friend","createdAt":"2026-10-19T19:29:45.747980Z","updatedAt":"2026-10-19T19:29:45.747980Z"}
```

`expectedSalaryCents` and `salaryCurrency` are optional, but must be given together.

| Field | Rules |
|---|---|
| `company`, `role` | required, not blank, at most 100 characters |
| `appliedDate` | required, today or earlier |
| `expectedSalaryCents` | optional, 0 or more |
| `salaryCurrency` | optional, ISO 4217 code in upper case, for example `EUR` |
| `notes` | optional, at most 1000 characters |

Invalid input returns `400 Bad Request` with an `errors` object that maps each invalid field to its message:

```bash
curl -i -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{"company":"  ","role":"Backend Engineer","appliedDate":"2026-10-01","expectedSalaryCents":5000000,"salaryCurrency":"eur"}'
```

```json
{"detail":"Request validation failed","instance":"/api/applications","status":400,"title":"Bad Request","errors":{"company":"must not be blank","salaryCurrency":"must be a three-letter upper-case currency code"}}
```

Malformed JSON, an unknown status value and an `id` that is not a UUID also return `400 Bad Request`.

An unexpected server error returns `500 Internal Server Error` with the detail "An unexpected error occurred". The cause is written to the application log, never to the response.

Get one application:

```bash
curl -i http://localhost:8080/api/applications/{id}
```

List applications. All query parameters are optional and can be combined:

| Parameter | Meaning |
|---|---|
| `status` | only applications with this status, for example `INTERVIEWING` |
| `company` | only companies whose name contains this text, ignoring case |
| `appliedFrom`, `appliedTo` | only applications applied on or after, and on or before, these dates (`yyyy-MM-dd`) |
| `page` | page number, starting at 0 (default 0) |
| `size` | page size (default 20, at most 100; larger values are reduced to 100) |
| `sort` | `field` or `field,asc` or `field,desc`; the fields are `appliedDate`, `company`, `status` and `createdAt` (default `createdAt,desc`); repeat the parameter to sort by more than one field |

```bash
curl -i "http://localhost:8080/api/applications?status=INTERVIEWING&company=acme&page=0&size=5&sort=appliedDate,desc"
```

The response is one page of results:

```json
{"content":[{"id":"cb079e49-447e-4eed-b2ff-f6036c910247","company":"Acme Labs","role":"Backend Engineer","status":"INTERVIEWING","appliedDate":"2026-09-04","expectedSalaryCents":null,"salaryCurrency":null,"notes":null,"createdAt":"2026-10-22T16:52:46.867131Z","updatedAt":"2026-10-22T16:52:47.302085Z"}],"page":0,"size":5,"totalElements":1,"totalPages":1}
```

Sorting by any other field, an unknown status, a date in another format, or `appliedFrom` after `appliedTo` returns `400 Bad Request`:

```json
{"detail":"Cannot sort by 'salary'. Allowed fields: appliedDate, company, status, createdAt","instance":"/api/applications","status":400,"title":"Bad Request"}
```

Update the details of an application:

```bash
curl -i -X PUT http://localhost:8080/api/applications/{id} \
  -H "Content-Type: application/json" \
  -d '{"company":"ACME Corp","role":"Backend Engineer","appliedDate":"2026-10-01","expectedSalaryCents":5500000,"salaryCurrency":"EUR","notes":"Recruiter call on Monday"}'
```

Change the status:

```bash
curl -i -X PATCH http://localhost:8080/api/applications/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status":"INTERVIEWING"}'
```

The allowed status changes are:

| From | To |
|---|---|
| `APPLIED` | `INTERVIEWING`, `REJECTED` |
| `INTERVIEWING` | `OFFER`, `REJECTED` |
| `OFFER` | none |
| `REJECTED` | none |

Any other change returns `409 Conflict`:

```bash
curl -i -X PATCH http://localhost:8080/api/applications/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status":"APPLIED"}'
```

```json
{"detail":"Cannot move from INTERVIEWING to APPLIED","instance":"/api/applications/0e85fcf9-d5c2-4e75-8e82-ad8efe50cb4a/status","status":409,"title":"Conflict"}
```

Every status an application has had is stored in the `application_status_changes` table: one entry when it is created and one for each allowed status change. Get the history, oldest first:

```bash
curl -i http://localhost:8080/api/applications/{id}/history
```

```json
[{"oldStatus":null,"newStatus":"APPLIED","changedAt":"2026-10-23T13:47:20.402240Z"},{"oldStatus":"APPLIED","newStatus":"INTERVIEWING","changedAt":"2026-10-23T13:47:20.555596Z"},{"oldStatus":"INTERVIEWING","newStatus":"OFFER","changedAt":"2026-10-23T13:47:20.583869Z"}]
```

Get statistics over all applications:

```bash
curl -i http://localhost:8080/api/applications/stats
```

```json
{"total":3,"active":1,"byStatus":{"APPLIED":1,"INTERVIEWING":0,"OFFER":1,"REJECTED":1},"topCompanies":[{"company":"acme corp","count":2},{"company":"globex","count":1}]}
```

`active` counts the applications in `APPLIED` or `INTERVIEWING`. `byStatus` always lists every status, with 0 when there are none. `topCompanies` lists at most five companies with the most applications; company names are compared ignoring case and shown in lower case, and companies with the same count are ordered by name. The numbers are computed with `GROUP BY` queries in PostgreSQL.

Delete an application. Its status history is deleted with it. The response is `204 No Content`, and a later `GET` returns `404 Not Found`:

```bash
curl -i -X DELETE http://localhost:8080/api/applications/{id}
curl -i http://localhost:8080/api/applications/{id}
```

Errors use the Problem Details format (RFC 9457) with the content type `application/problem+json`:

```json
{"detail":"No application found with id: 0e85fcf9-d5c2-4e75-8e82-ad8efe50cb4a","instance":"/api/applications/0e85fcf9-d5c2-4e75-8e82-ad8efe50cb4a","status":404,"title":"Not Found"}
```

## Test

```bash
./mvnw --batch-mode clean verify
```

The repository and application tests start a PostgreSQL 17 container with Testcontainers, so Docker must be running. The service tests use Mockito and the controller tests use MockMvc with a mocked service, so both run without a database. The same command runs in GitHub Actions on every push.
