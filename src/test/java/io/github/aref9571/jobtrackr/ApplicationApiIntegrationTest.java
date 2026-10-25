package io.github.aref9571.jobtrackr;

import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ApplicationStatisticsResponse;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CompanyCount;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.StatusChangeResponse;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ApplicationApiIntegrationTest {

    private static final LocalDate APPLIED_DATE = LocalDate.of(2026, 10, 1);

    @Autowired
    private RestTestClient restClient;

    @Test
    void applicationLifecycle_fromCreateToDelete_worksAgainstDatabase() {
        CreateApplicationRequest createRequest = new CreateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend");
        UpdateApplicationRequest updateRequest = new UpdateApplicationRequest(
                "ACME Corp", "Senior Backend Engineer", APPLIED_DATE, 5_500_000L, "EUR", "Recruiter call on Monday");

        EntityExchangeResult<ApplicationResponse> createResult = restClient.post().uri("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ApplicationResponse.class)
                .returnResult();
        ApplicationResponse created = createResult.getResponseBody();
        UUID id = created.id();

        assertThat(created.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(createResult.getResponseHeaders().getLocation()).hasPath("/api/applications/" + id);

        ApplicationResponse loaded = getApplication(id);

        assertThat(loaded).isEqualTo(created);

        ApplicationResponse updated = restClient.put().uri("/api/applications/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated.role()).isEqualTo("Senior Backend Engineer");
        assertThat(updated.expectedSalaryCents()).isEqualTo(5_500_000L);
        assertThat(updated.notes()).isEqualTo("Recruiter call on Monday");
        assertThat(updated.status()).isEqualTo(ApplicationStatus.APPLIED);

        ApplicationResponse interviewing = changeStatus(id, ApplicationStatus.INTERVIEWING);
        ApplicationResponse offer = changeStatus(id, ApplicationStatus.OFFER);

        assertThat(interviewing.status()).isEqualTo(ApplicationStatus.INTERVIEWING);
        assertThat(offer.status()).isEqualTo(ApplicationStatus.OFFER);
        assertThat(getApplication(id).status()).isEqualTo(ApplicationStatus.OFFER);

        List<StatusChangeResponse> history = restClient.get().uri("/api/applications/{id}/history", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<StatusChangeResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(history)
                .extracting(StatusChangeResponse::oldStatus, StatusChangeResponse::newStatus)
                .containsExactly(
                        tuple(null, ApplicationStatus.APPLIED),
                        tuple(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEWING),
                        tuple(ApplicationStatus.INTERVIEWING, ApplicationStatus.OFFER));

        ApplicationStatisticsResponse statistics = restClient.get().uri("/api/applications/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationStatisticsResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(statistics.total()).isEqualTo(1);
        assertThat(statistics.active()).isZero();
        assertThat(statistics.byStatus()).containsEntry(ApplicationStatus.OFFER, 1L)
                .containsEntry(ApplicationStatus.APPLIED, 0L)
                .hasSize(ApplicationStatus.values().length);
        assertThat(statistics.topCompanies()).containsExactly(new CompanyCount("acme corp", 1));

        restClient.delete().uri("/api/applications/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        restClient.get().uri("/api/applications/{id}", id)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
        restClient.get().uri("/api/applications/{id}/history", id)
                .exchange()
                .expectStatus().isNotFound();
    }

    private ApplicationResponse getApplication(UUID id) {
        return restClient.get().uri("/api/applications/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ApplicationResponse changeStatus(UUID id, ApplicationStatus status) {
        return restClient.patch().uri("/api/applications/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeStatusRequest(status))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();
    }
}
