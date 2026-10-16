package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.TestcontainersConfiguration;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class JobApplicationRepositoryTest {

    private static final LocalDate APPLIED_DATE = LocalDate.of(2026, 10, 1);

    @Autowired
    private JobApplicationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_newApplication_canBeFoundById() {
        JobApplication application = new JobApplication(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend");

        repository.save(application);
        entityManager.flush();
        entityManager.clear();
        Optional<JobApplication> found = repository.findById(application.getId());

        assertThat(found).isPresent();
        JobApplication loaded = found.get();
        assertThat(loaded.getCompany()).isEqualTo("ACME Corp");
        assertThat(loaded.getRole()).isEqualTo("Backend Engineer");
        assertThat(loaded.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(loaded.getAppliedDate()).isEqualTo(APPLIED_DATE);
        assertThat(loaded.getExpectedSalaryCents()).isEqualTo(5_000_000L);
        assertThat(loaded.getSalaryCurrency()).isEqualTo("EUR");
        assertThat(loaded.getNotes()).isEqualTo("Referred by a friend");
    }

    @Test
    void save_withoutOptionalFields_storesNulls() {
        JobApplication application = new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null);

        repository.save(application);
        entityManager.flush();
        entityManager.clear();
        JobApplication loaded = repository.findById(application.getId()).orElseThrow();

        assertThat(loaded.getExpectedSalaryCents()).isNull();
        assertThat(loaded.getSalaryCurrency()).isNull();
        assertThat(loaded.getNotes()).isNull();
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        Optional<JobApplication> found = repository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByCompanyAndRole_sameValuesInDifferentCase_returnsTrue() {
        repository.saveAndFlush(new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));

        boolean exists = repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("acme corp", "BACKEND ENGINEER");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByCompanyAndRole_differentRole_returnsFalse() {
        repository.saveAndFlush(new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));

        boolean exists = repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("ACME Corp", "Frontend Engineer");

        assertThat(exists).isFalse();
    }

    @Test
    void saveAndFlush_sameCompanyAndRoleInDifferentCase_violatesUniqueIndex() {
        repository.saveAndFlush(new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));
        JobApplication duplicate = new JobApplication("acme corp", "backend engineer", APPLIED_DATE, null, null, null);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_newApplication_setsCreatedAndUpdatedTimestamps() {
        Instant beforeSave = Instant.now();
        JobApplication application = new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null);

        JobApplication saved = repository.saveAndFlush(application);

        assertThat(saved.getCreatedAt()).isNotNull().isAfterOrEqualTo(beforeSave);
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void save_changedStatus_updatesOnlyUpdatedTimestamp() {
        JobApplication saved = repository.saveAndFlush(
                new JobApplication("ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));
        Instant createdAt = saved.getCreatedAt();

        saved.changeStatus(ApplicationStatus.INTERVIEWING);
        repository.saveAndFlush(saved);

        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getUpdatedAt()).isAfter(createdAt);
    }
}
