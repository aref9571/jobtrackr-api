package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.TestcontainersConfiguration;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;
import io.github.aref9571.jobtrackr.model.StatusChange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class StatusChangeRepositoryTest {

    private static final LocalDate APPLIED_DATE = LocalDate.of(2026, 10, 1);

    @Autowired
    private StatusChangeRepository statusChangeRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByApplicationId_severalChanges_returnsOnlyThatApplicationOldestFirst() {
        JobApplication application = saveApplication("ACME Corp");
        JobApplication otherApplication = saveApplication("Globex");
        saveChange(application, null, ApplicationStatus.APPLIED);
        saveChange(otherApplication, null, ApplicationStatus.APPLIED);
        saveChange(application, ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEWING);
        saveChange(application, ApplicationStatus.INTERVIEWING, ApplicationStatus.OFFER);
        entityManager.clear();

        List<StatusChange> history = statusChangeRepository.findByApplicationIdOrderByChangedAtAsc(application.getId());

        assertThat(history)
                .extracting(StatusChange::getOldStatus, StatusChange::getNewStatus)
                .containsExactly(
                        tuple(null, ApplicationStatus.APPLIED),
                        tuple(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEWING),
                        tuple(ApplicationStatus.INTERVIEWING, ApplicationStatus.OFFER));
        assertThat(history.get(0).getChangedAt()).isBeforeOrEqualTo(history.get(1).getChangedAt());
        assertThat(history.get(1).getChangedAt()).isBeforeOrEqualTo(history.get(2).getChangedAt());
    }

    @Test
    void findByApplicationId_noChanges_returnsEmptyList() {
        JobApplication application = saveApplication("ACME Corp");

        List<StatusChange> history = statusChangeRepository.findByApplicationIdOrderByChangedAtAsc(application.getId());

        assertThat(history).isEmpty();
    }

    @Test
    void save_statusChange_storesChangedAtAsGiven() {
        JobApplication application = saveApplication("ACME Corp");
        StatusChange saved = saveChange(application, null, ApplicationStatus.APPLIED);
        entityManager.clear();

        StatusChange loaded = statusChangeRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getChangedAt()).isEqualTo(saved.getChangedAt());
    }

    @Test
    void deleteApplication_withHistory_deletesHistoryThroughCascade() {
        JobApplication application = saveApplication("ACME Corp");
        JobApplication otherApplication = saveApplication("Globex");
        saveChange(application, null, ApplicationStatus.APPLIED);
        saveChange(application, ApplicationStatus.APPLIED, ApplicationStatus.REJECTED);
        saveChange(otherApplication, null, ApplicationStatus.APPLIED);
        entityManager.clear();

        applicationRepository.deleteById(application.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(statusChangeRepository.findByApplicationIdOrderByChangedAtAsc(application.getId())).isEmpty();
        assertThat(statusChangeRepository.findByApplicationIdOrderByChangedAtAsc(otherApplication.getId())).hasSize(1);
    }

    private JobApplication saveApplication(String company) {
        return applicationRepository.saveAndFlush(
                new JobApplication(company, "Backend Engineer", APPLIED_DATE, null, null, null));
    }

    private StatusChange saveChange(JobApplication application, ApplicationStatus oldStatus,
                                    ApplicationStatus newStatus) {
        return statusChangeRepository.saveAndFlush(new StatusChange(application, oldStatus, newStatus));
    }
}
