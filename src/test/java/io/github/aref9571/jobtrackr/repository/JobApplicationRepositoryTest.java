package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.TestcontainersConfiguration;
import io.github.aref9571.jobtrackr.dto.CompanyCount;
import io.github.aref9571.jobtrackr.dto.StatusCount;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;
import io.github.aref9571.jobtrackr.model.Role;
import io.github.aref9571.jobtrackr.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class JobApplicationRepositoryTest {

    private static final LocalDate APPLIED_DATE = LocalDate.of(2026, 10, 1);
    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuuS7Ir1Dx5l6xNQ7r5BkcGcCL6kK1yJ0O";

    @Autowired
    private JobApplicationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private UUID ownerId;

    @BeforeEach
    void saveOwner() {
        ownerId = userRepository.saveAndFlush(new User("ann@example.com", PASSWORD_HASH, Role.USER)).getId();
    }

    @Test
    void save_newApplication_canBeFoundById() {
        JobApplication application = new JobApplication(
                ownerId, "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend");

        repository.save(application);
        entityManager.flush();
        entityManager.clear();
        Optional<JobApplication> found = repository.findById(application.getId());

        assertThat(found).isPresent();
        JobApplication loaded = found.get();
        assertThat(loaded.getOwnerId()).isEqualTo(ownerId);
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
        JobApplication application = new JobApplication(
                ownerId, "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null);

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
        repository.saveAndFlush(new JobApplication(
                ownerId, "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));

        boolean exists = repository.existsByOwnerIdAndCompanyIgnoreCaseAndRoleIgnoreCase(
                ownerId, "acme corp", "BACKEND ENGINEER");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByCompanyAndRole_differentRole_returnsFalse() {
        repository.saveAndFlush(new JobApplication(
                ownerId, "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));

        boolean exists = repository.existsByOwnerIdAndCompanyIgnoreCaseAndRoleIgnoreCase(
                ownerId, "ACME Corp", "Frontend Engineer");

        assertThat(exists).isFalse();
    }

    @Test
    void saveAndFlush_sameCompanyAndRoleInDifferentCase_violatesUniqueIndex() {
        repository.saveAndFlush(new JobApplication(
                ownerId, "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));
        JobApplication duplicate = new JobApplication(
                ownerId, "acme corp", "backend engineer", APPLIED_DATE, null, null, null);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_newApplication_setsCreatedAndUpdatedTimestamps() {
        Instant beforeSave = Instant.now();
        JobApplication application = new JobApplication(
                ownerId, "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null);

        JobApplication saved = repository.saveAndFlush(application);

        assertThat(saved.getCreatedAt()).isNotNull().isAfterOrEqualTo(beforeSave);
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void saveAndFlush_newApplication_returnsTimestampsAsStored() {
        JobApplication saved = repository.saveAndFlush(
                new JobApplication(ownerId, "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));
        entityManager.clear();

        JobApplication loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(loaded.getUpdatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void save_changedStatus_updatesOnlyUpdatedTimestamp() {
        JobApplication saved = repository.saveAndFlush(
                new JobApplication(ownerId, "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null));
        Instant createdAt = saved.getCreatedAt();

        saved.changeStatus(ApplicationStatus.INTERVIEWING);
        repository.saveAndFlush(saved);

        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getUpdatedAt()).isAfter(createdAt);
    }

    @Test
    void countPerStatus_mixedStatuses_returnsOneRowPerUsedStatus() {
        saveWithStatus("ACME Corp", "Backend Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("Globex", "Backend Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("Initech", "Backend Engineer", ApplicationStatus.INTERVIEWING);
        saveWithStatus("Hooli", "Backend Engineer", ApplicationStatus.REJECTED);

        List<StatusCount> counts = repository.countPerStatus(ownerId);

        assertThat(counts).containsExactlyInAnyOrder(
                new StatusCount(ApplicationStatus.APPLIED, 2),
                new StatusCount(ApplicationStatus.INTERVIEWING, 1),
                new StatusCount(ApplicationStatus.REJECTED, 1));
    }

    @Test
    void countPerStatus_noApplications_returnsEmptyList() {
        List<StatusCount> counts = repository.countPerStatus(ownerId);

        assertThat(counts).isEmpty();
    }

    @Test
    void findTopCompanies_sameCompanyInDifferentCase_countsThemTogether() {
        saveWithStatus("ACME Corp", "Backend Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("acme corp", "Java Developer", ApplicationStatus.APPLIED);
        saveWithStatus("Globex", "Backend Engineer", ApplicationStatus.APPLIED);

        List<CompanyCount> topCompanies = repository.findTopCompanies(ownerId, Limit.of(5));

        assertThat(topCompanies).containsExactly(
                new CompanyCount("acme corp", 2),
                new CompanyCount("globex", 1));
    }

    @Test
    void findTopCompanies_moreCompaniesThanLimit_returnsMostFrequentWithTiesByName() {
        saveWithStatus("Umbrella", "Backend Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("Umbrella", "Java Developer", ApplicationStatus.APPLIED);
        saveWithStatus("Umbrella", "Platform Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("Hooli", "Backend Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("Hooli", "Java Developer", ApplicationStatus.APPLIED);
        saveWithStatus("Globex", "Backend Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("Initech", "Backend Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("ACME Corp", "Backend Engineer", ApplicationStatus.APPLIED);
        saveWithStatus("Soylent", "Backend Engineer", ApplicationStatus.APPLIED);

        List<CompanyCount> topCompanies = repository.findTopCompanies(ownerId, Limit.of(5));

        assertThat(topCompanies).containsExactly(
                new CompanyCount("umbrella", 3),
                new CompanyCount("hooli", 2),
                new CompanyCount("acme corp", 1),
                new CompanyCount("globex", 1),
                new CompanyCount("initech", 1));
    }

    @Test
    void findTopCompanies_noApplications_returnsEmptyList() {
        List<CompanyCount> topCompanies = repository.findTopCompanies(ownerId, Limit.of(5));

        assertThat(topCompanies).isEmpty();
    }

    private void saveWithStatus(String company, String role, ApplicationStatus status) {
        JobApplication application = new JobApplication(ownerId, company, role, APPLIED_DATE, null, null, null);
        if (status != ApplicationStatus.APPLIED) {
            application.changeStatus(status);
        }
        repository.saveAndFlush(application);
    }
}
