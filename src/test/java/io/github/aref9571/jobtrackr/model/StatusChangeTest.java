package io.github.aref9571.jobtrackr.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusChangeTest {

    private static final JobApplication APPLICATION =
            new JobApplication("ACME Corp", "Backend Engineer", LocalDate.of(2026, 10, 1), null, null, null);

    @Test
    void constructor_validValues_createsStatusChange() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

        StatusChange statusChange = new StatusChange(
                APPLICATION, ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEWING);

        assertThat(statusChange.getId()).isNotNull();
        assertThat(statusChange.getApplication()).isSameAs(APPLICATION);
        assertThat(statusChange.getOldStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(statusChange.getNewStatus()).isEqualTo(ApplicationStatus.INTERVIEWING);
        assertThat(statusChange.getChangedAt()).isAfterOrEqualTo(before);
        assertThat(statusChange.getChangedAt().getNano() % 1_000).isZero();
    }

    @Test
    void constructor_firstEntry_allowsMissingOldStatus() {
        StatusChange statusChange = new StatusChange(APPLICATION, null, ApplicationStatus.APPLIED);

        assertThat(statusChange.getOldStatus()).isNull();
        assertThat(statusChange.getNewStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void constructor_nullApplication_throwsException() {
        assertThatThrownBy(() -> new StatusChange(null, null, ApplicationStatus.APPLIED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("application must not be null");
    }

    @Test
    void constructor_nullNewStatus_throwsException() {
        assertThatThrownBy(() -> new StatusChange(APPLICATION, ApplicationStatus.APPLIED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("newStatus must not be null");
    }
}
