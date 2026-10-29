package io.github.aref9571.jobtrackr.dto;

import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationResponseTest {

    private static final UUID OWNER_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Test
    void from_application_copiesAllFields() {
        JobApplication application = new JobApplication(
                OWNER_ID, "ACME Corp", "Backend Engineer", LocalDate.of(2026, 10, 1),
                5_000_000L, "EUR", "Referred by a friend");
        application.changeStatus(ApplicationStatus.INTERVIEWING);

        ApplicationResponse response = ApplicationResponse.from(application);

        assertThat(response.id()).isEqualTo(application.getId());
        assertThat(response.company()).isEqualTo("ACME Corp");
        assertThat(response.role()).isEqualTo("Backend Engineer");
        assertThat(response.status()).isEqualTo(ApplicationStatus.INTERVIEWING);
        assertThat(response.appliedDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(response.expectedSalaryCents()).isEqualTo(5_000_000L);
        assertThat(response.salaryCurrency()).isEqualTo("EUR");
        assertThat(response.notes()).isEqualTo("Referred by a friend");
        assertThat(response.createdAt()).isEqualTo(application.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(application.getUpdatedAt());
    }
}
