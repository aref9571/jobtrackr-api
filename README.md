# JobTrackr API

JobTrackr API is a Spring Boot REST API for tracking job applications. It is being built step by step from the JobTrackr domain in [java-foundations](https://github.com/aref9571/java-foundations). At this stage the project contains the job application domain model, its PostgreSQL table managed by Flyway, a Spring Data repository, a service layer with the application use cases, REST endpoints for creating, reading, updating, deleting and changing the status of job applications, request validation with consistent error responses, a list endpoint with filters, paging and sorting, a status history for each application, statistics computed in the database, user registration with BCrypt password hashing, login with JSON Web Tokens (JWT) that protect every application endpoint, applications that belong to the user who created them so that every user sees only their own data, an admin endpoint with totals across all users, OpenAPI documentation with Swagger UI, a health check endpoint, an end-to-end API test, a coverage report and a continuous integration build.

## Tech stack

- Java 21
- Spring Boot 4.1.1 (Spring Web MVC, Validation, Actuator, Spring Data JPA, Spring Security with the OAuth2 resource server for JWT)
- springdoc-openapi with Swagger UI
- PostgreSQL 17 with Flyway migrations
- Maven with the Maven Wrapper
- JUnit 5, Mockito and Testcontainers
- JaCoCo for test coverage
- Docker Compose for the local database
- GitHub Actions

## Requirements

- JDK 21
- Docker with Docker Compose

Maven does not need to be installed. The Maven Wrapper downloads the right version.

## Configuration

The application reads the database settings and the token signing secret from environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobtrackr` | JDBC URL of the database |
| `DB_USERNAME` | `jobtrackr` | Database user |
| `DB_PASSWORD` | none | Database password, must be set |
| `JWT_SECRET` | none | Secret used to sign and check login tokens (HS256), must be set and at least 32 bytes long |

Copy `.env.example` to `.env`, choose a database password and set `JWT_SECRET` to a random value. Docker Compose reads `.env` to create the database user. The `.env` file is ignored by Git.

```bash
cp .env.example .env
openssl rand -base64 32
```

Put the output of `openssl rand -base64 32` into `JWT_SECRET`. The application does not start when `JWT_SECRET` is missing or shorter than 32 bytes. Tokens are valid for one hour (`jobtrackr.jwt.expiration` in `application.yml`).

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

## API documentation

While the application is running, Swagger UI lists every endpoint with its parameters, request bodies and responses, and can send requests to the running application:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI description in JSON: http://localhost:8080/v3/api-docs

To call the application endpoints from Swagger UI, log in with `POST /api/auth/login`, click "Authorize" and paste the `accessToken` value.

## Endpoints

| Method | Path | Token | Success | Errors |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | no | 201 with `id` and `email` | 400, 409 email already registered |
| `POST` | `/api/auth/login` | no | 200 with `accessToken`, `tokenType` and `expiresIn` | 400, 401 invalid email or password |
| `POST` | `/api/applications` | yes | 201 with a `Location` header | 400, 401, 409 duplicate |
| `GET` | `/api/applications` | yes | 200 with one page of results | 400, 401 |
| `GET` | `/api/applications/{id}` | yes | 200 | 400, 401, 404 |
| `PUT` | `/api/applications/{id}` | yes | 200 | 400, 401, 404, 409 duplicate |
| `PATCH` | `/api/applications/{id}/status` | yes | 200 | 400, 401, 404, 409 status change not allowed |
| `GET` | `/api/applications/{id}/history` | yes | 200 | 400, 401, 404 |
| `GET` | `/api/applications/stats` | yes | 200 | 401 |
| `DELETE` | `/api/applications/{id}` | yes | 204 | 400, 401, 404 |
| `GET` | `/api/admin/stats` | yes, role `ADMIN` | 200 with `totalUsers` and `totalApplications` | 401, 403 |
| `GET` | `/actuator/health` | no | 200 | |
| `GET` | `/swagger-ui.html` | no | 302 to Swagger UI | |
| `GET` | `/v3/api-docs` | no | 200 with the OpenAPI description | |

Registration, login, the health check and the API documentation are public. Every other path needs a valid token in the `Authorization: Bearer <token>` header. A request without a token, with a token that is not a valid JWT, with a wrong signature or with an expired token returns `401 Unauthorized` with an empty body and a `WWW-Authenticate` header.

## Ownership and roles

Every application belongs to the user who created it. The API takes the user id from the `sub` claim of the token, never from the request, and every application query is limited to that user: list, get, update, status change, delete, history, statistics, search and the duplicate check. Two users can therefore both track the same company and role, while one user still gets `409 Conflict` for a second application with the same company and role.

Another user's application gives the same `404 Not Found` as an id that does not exist, so the API does not reveal whether it exists.

Every new user has the role `USER`. `GET /api/admin/stats` needs the role `ADMIN` and returns `403 Forbidden` for other users. There is no endpoint to change roles. To make a user an admin, update the `users` table, for example in the Docker Compose database:

```bash
docker compose exec postgres psql -U jobtrackr -d jobtrackr \
  -c "update users set role = 'ADMIN' where email = 'ann.smith@example.com';"
```

The role is stored in the token at login, so the user must log in again to get a token with the new role. An admin still sees only their own applications in the `/api/applications` endpoints.

## API examples

All examples assume the application is running on port 8080. Replace `{id}` with the `id` returned when the application was created. The application endpoints need the token from the login example.

Register a user:

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"Ann.Smith@Example.com","password":"correct-horse-battery"}'
```

The response is `201 Created` with the new user's id and email. The password is never returned:

```json
{"id":"ef5997a0-f5f7-4f82-8187-16a364ccd7dc","email":"ann.smith@example.com"}
```

| Field | Rules |
|---|---|
| `email` | required, a valid email address, at most 254 characters; stored in lower case |
| `password` | required, 8 to 72 characters and at most 72 bytes in UTF-8 |

The password is stored only as a BCrypt hash in the `users` table. Emails are compared ignoring case, so registering `ANN.SMITH@EXAMPLE.COM` afterwards returns `409 Conflict`:

```json
{"detail":"Email is already registered","instance":"/api/auth/register","status":409,"title":"Conflict"}
```

Log in with the same email and password:

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ann.smith@example.com","password":"correct-horse-battery"}'
```

The response is `200 OK` with a signed JWT that is valid for 3600 seconds:

```json
{"accessToken":"eyJraWQiOi...","tokenType":"Bearer","expiresIn":3600}
```

The token is signed with HS256 and contains the user id as `sub`, the user's roles as `roles`, and the times `iat` (issued at) and `exp` (expires at). A wrong password and an unknown email give the same answer:

```json
{"detail":"Invalid email or password","instance":"/api/auth/login","status":401,"title":"Unauthorized"}
```

Keep the token in a shell variable for the next examples:

```bash
TOKEN=<accessToken from the login response>
```

Create an application:

```bash
curl -i -X POST http://localhost:8080/api/applications \
  -H "Authorization: Bearer $TOKEN" \
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
  -H "Authorization: Bearer $TOKEN" \
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
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/applications/{id}
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
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/applications?status=INTERVIEWING&company=acme&page=0&size=5&sort=appliedDate,desc"
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
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"company":"ACME Corp","role":"Backend Engineer","appliedDate":"2026-10-01","expectedSalaryCents":5500000,"salaryCurrency":"EUR","notes":"Recruiter call on Monday"}'
```

Change the status:

```bash
curl -i -X PATCH http://localhost:8080/api/applications/{id}/status \
  -H "Authorization: Bearer $TOKEN" \
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
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"APPLIED"}'
```

```json
{"detail":"Cannot move from INTERVIEWING to APPLIED","instance":"/api/applications/0e85fcf9-d5c2-4e75-8e82-ad8efe50cb4a/status","status":409,"title":"Conflict"}
```

Every status an application has had is stored in the `application_status_changes` table: one entry when it is created and one for each allowed status change. Get the history, oldest first:

```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/applications/{id}/history
```

```json
[{"oldStatus":null,"newStatus":"APPLIED","changedAt":"2026-10-23T13:47:20.402240Z"},{"oldStatus":"APPLIED","newStatus":"INTERVIEWING","changedAt":"2026-10-23T13:47:20.555596Z"},{"oldStatus":"INTERVIEWING","newStatus":"OFFER","changedAt":"2026-10-23T13:47:20.583869Z"}]
```

Get statistics over your own applications:

```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/applications/stats
```

```json
{"total":3,"active":1,"byStatus":{"APPLIED":1,"INTERVIEWING":0,"OFFER":1,"REJECTED":1},"topCompanies":[{"company":"acme corp","count":2},{"company":"globex","count":1}]}
```

`active` counts the applications in `APPLIED` or `INTERVIEWING`. `byStatus` always lists every status, with 0 when there are none. `topCompanies` lists at most five companies with the most applications; company names are compared ignoring case and shown in lower case, and companies with the same count are ordered by name. The numbers are computed with `GROUP BY` queries in PostgreSQL.

Delete an application. Its status history is deleted with it. The response is `204 No Content`, and a later `GET` returns `404 Not Found`:

```bash
curl -i -H "Authorization: Bearer $TOKEN" -X DELETE http://localhost:8080/api/applications/{id}
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/applications/{id}
```

Log in as a user with the role `ADMIN` (see [Ownership and roles](#ownership-and-roles)) and keep that token in `ADMIN_TOKEN`. Get the number of users and applications across all users:

```bash
curl -i -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/admin/stats
```

```json
{"totalUsers":2,"totalApplications":3}
```

Errors use the Problem Details format (RFC 9457) with the content type `application/problem+json`:

```json
{"detail":"No application found with id: 0e85fcf9-d5c2-4e75-8e82-ad8efe50cb4a","instance":"/api/applications/0e85fcf9-d5c2-4e75-8e82-ad8efe50cb4a","status":404,"title":"Not Found"}
```

## Running tests

Run all tests and build the coverage report:

```bash
./mvnw --batch-mode clean verify
```

Docker must be running, because some tests start a PostgreSQL 17 container with Testcontainers. The tests use a fixed test-only `jobtrackr.jwt.secret` from `src/test/resources/application.properties`, so `JWT_SECRET` is not needed to run them.

| Kind | Classes | What runs |
|---|---|---|
| Unit tests | `model`, `dto` and `service` tests | Plain JUnit tests; the service tests replace the repositories with Mockito mocks |
| Repository tests | `repository` tests | `@DataJpaTest` against PostgreSQL in Testcontainers, with the Flyway migrations |
| Controller tests | `ApplicationControllerTest`, `AuthControllerTest`, `AdminControllerTest` | `@WebMvcTest` with MockMvc, the security configuration and a mocked service, no database; requests get a test JWT with a user id or a role from Spring Security's `jwt()` request post processor |
| Context test | `JobtrackrApiApplicationTests` | Starts the application context against PostgreSQL in Testcontainers |
| Integration test | `ApplicationApiIntegrationTest` | The whole application on a random port against PostgreSQL in Testcontainers, called over HTTP with `RestTestClient`: register, log in, then create, get, update, change status, history, statistics and delete with the token |
| Integration test | `AuthApiIntegrationTest` | Registration and login over HTTP against PostgreSQL, the stored BCrypt hash, duplicate emails in another case, the token claims, the same 401 for a wrong password and an unknown email, requests without a token or with a broken, expired or foreign token, and which paths are public |
| Integration test | `OwnershipApiIntegrationTest` | Two users over HTTP against PostgreSQL: one user cannot read, update, change the status of, delete or see the history of the other user's application (404), list, search and statistics only include the user's own applications, the duplicate rule applies per user, and `/api/admin/stats` returns 401 without a token, 403 for `USER` and 200 for `ADMIN` |

Run one test class:

```bash
./mvnw --batch-mode test -Dtest=ApplicationApiIntegrationTest
```

`verify` also writes a JaCoCo coverage report to `target/site/jacoco/index.html`. GitHub Actions runs the same command on every push and uploads the report as the `jacoco-report` artifact of the workflow run.
