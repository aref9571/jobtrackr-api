package io.github.aref9571.jobtrackr;

import io.github.aref9571.jobtrackr.dto.AdminStatisticsResponse;
import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ApplicationStatisticsResponse;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CompanyCount;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.LoginRequest;
import io.github.aref9571.jobtrackr.dto.LoginResponse;
import io.github.aref9571.jobtrackr.dto.PageResponse;
import io.github.aref9571.jobtrackr.dto.RegisterRequest;
import io.github.aref9571.jobtrackr.dto.StatusChangeResponse;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class OwnershipApiIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";
    private static final LocalDate APPLIED_DATE = LocalDate.of(2026, 10, 1);

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestTestClient annClient;
    private RestTestClient bobClient;

    @BeforeEach
    void registerTwoUsers() {
        annClient = clientFor(registerNewUser("ann"));
        bobClient = clientFor(registerNewUser("bob"));
    }

    @Test
    void otherUsersApplication_everyEndpoint_returns404AndLeavesItUnchanged() {
        ApplicationResponse annApplication = create(annClient, "ACME Corp", "Backend Engineer");
        UUID id = annApplication.id();

        bobClient.get().uri("/api/applications/{id}", id)
                .exchange()
                .expectStatus().isNotFound();
        bobClient.put().uri("/api/applications/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateApplicationRequest("Globex", "Java Developer", APPLIED_DATE, null, null, "changed"))
                .exchange()
                .expectStatus().isNotFound();
        bobClient.patch().uri("/api/applications/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeStatusRequest(ApplicationStatus.REJECTED))
                .exchange()
                .expectStatus().isNotFound();
        bobClient.get().uri("/api/applications/{id}/history", id)
                .exchange()
                .expectStatus().isNotFound();
        bobClient.delete().uri("/api/applications/{id}", id)
                .exchange()
                .expectStatus().isNotFound();

        ApplicationResponse afterAttempts = annClient.get().uri("/api/applications/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(afterAttempts).isEqualTo(annApplication);
        assertThat(history(annClient, id)).extracting(StatusChangeResponse::newStatus)
                .containsExactly(ApplicationStatus.APPLIED);
    }

    @Test
    void otherUsersApplication_returnsSame404AsUnknownId() {
        UUID annApplicationId = create(annClient, "ACME Corp", "Backend Engineer").id();
        UUID unknownId = UUID.randomUUID();

        ProblemDetail otherUsers = getProblem(bobClient, annApplicationId);
        ProblemDetail unknown = getProblem(bobClient, unknownId);

        assertThat(otherUsers.getStatus()).isEqualTo(unknown.getStatus()).isEqualTo(404);
        assertThat(otherUsers.getDetail()).isEqualTo("No application found with id: " + annApplicationId);
        assertThat(unknown.getDetail()).isEqualTo("No application found with id: " + unknownId);
    }

    @Test
    void searchAndStatistics_twoUsers_onlyIncludeOwnApplications() {
        create(annClient, "ACME Corp", "Backend Engineer");
        create(annClient, "Globex", "Backend Engineer");
        create(bobClient, "ACME Corp", "Java Developer");

        PageResponse<ApplicationResponse> annPage = search(annClient, "/api/applications");
        PageResponse<ApplicationResponse> bobAcmePage = search(bobClient, "/api/applications?company=acme");

        assertThat(annPage.totalElements()).isEqualTo(2);
        assertThat(annPage.content()).extracting(ApplicationResponse::company)
                .containsExactlyInAnyOrder("ACME Corp", "Globex");
        assertThat(bobAcmePage.totalElements()).isEqualTo(1);
        assertThat(bobAcmePage.content()).extracting(ApplicationResponse::role).containsExactly("Java Developer");

        ApplicationStatisticsResponse annStatistics = statistics(annClient);
        ApplicationStatisticsResponse bobStatistics = statistics(bobClient);

        assertThat(annStatistics.total()).isEqualTo(2);
        assertThat(annStatistics.byStatus()).containsEntry(ApplicationStatus.APPLIED, 2L);
        assertThat(annStatistics.topCompanies()).containsExactly(
                new CompanyCount("acme corp", 1), new CompanyCount("globex", 1));
        assertThat(bobStatistics.total()).isEqualTo(1);
        assertThat(bobStatistics.topCompanies()).containsExactly(new CompanyCount("acme corp", 1));
    }

    @Test
    void createApplication_sameCompanyAndRoleForTwoUsers_isAllowedButNotTwiceForOneUser() {
        create(annClient, "ACME Corp", "Backend Engineer");
        create(bobClient, "acme corp", "backend engineer");

        annClient.post().uri("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateApplicationRequest("ACME CORP", "BACKEND ENGINEER", APPLIED_DATE, null, null, null))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void adminStatistics_withoutToken_returns401() {
        restClient.get().uri("/api/admin/stats")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void adminStatistics_userRole_returns403() {
        annClient.get().uri("/api/admin/stats")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminStatistics_adminRole_returnsTotalsAcrossAllUsers() {
        create(annClient, "ACME Corp", "Backend Engineer");
        create(bobClient, "Globex", "Backend Engineer");
        String adminEmail = registerNewUser("admin");
        jdbcTemplate.update("update users set role = 'ADMIN' where email = ?", adminEmail);
        RestTestClient adminClient = clientFor(adminEmail);

        AdminStatisticsResponse statistics = adminClient.get().uri("/api/admin/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AdminStatisticsResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(statistics.totalUsers()).isEqualTo(countRows("users")).isGreaterThanOrEqualTo(3);
        assertThat(statistics.totalApplications()).isEqualTo(countRows("job_applications")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void adminRole_applicationEndpoints_stillShowOnlyOwnApplications() {
        create(annClient, "ACME Corp", "Backend Engineer");
        String adminEmail = registerNewUser("admin");
        jdbcTemplate.update("update users set role = 'ADMIN' where email = ?", adminEmail);
        RestTestClient adminClient = clientFor(adminEmail);

        PageResponse<ApplicationResponse> adminPage = search(adminClient, "/api/applications");

        assertThat(adminPage.totalElements()).isZero();
    }

    private String registerNewUser(String name) {
        String email = name + "-" + UUID.randomUUID() + "@example.com";
        restClient.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isCreated();
        return email;
    }

    private RestTestClient clientFor(String email) {
        LoginResponse login = restClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        return restClient.mutate()
                .defaultHeaders(headers -> headers.setBearerAuth(login.accessToken()))
                .build();
    }

    private static ApplicationResponse create(RestTestClient client, String company, String role) {
        return client.post().uri("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateApplicationRequest(company, role, APPLIED_DATE, null, null, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private static List<StatusChangeResponse> history(RestTestClient client, UUID id) {
        return client.get().uri("/api/applications/{id}/history", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<StatusChangeResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private static ProblemDetail getProblem(RestTestClient client, UUID id) {
        return client.get().uri("/api/applications/{id}", id)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();
    }

    private static PageResponse<ApplicationResponse> search(RestTestClient client, String uri) {
        return client.get().uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<ApplicationResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private static ApplicationStatisticsResponse statistics(RestTestClient client) {
        return client.get().uri("/api/applications/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationStatisticsResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private long countRows(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }
}
