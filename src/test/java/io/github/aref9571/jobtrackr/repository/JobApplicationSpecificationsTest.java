package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.TestcontainersConfiguration;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;
import io.github.aref9571.jobtrackr.model.Role;
import io.github.aref9571.jobtrackr.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.github.aref9571.jobtrackr.repository.JobApplicationSpecifications.appliedOnOrAfter;
import static io.github.aref9571.jobtrackr.repository.JobApplicationSpecifications.appliedOnOrBefore;
import static io.github.aref9571.jobtrackr.repository.JobApplicationSpecifications.companyContains;
import static io.github.aref9571.jobtrackr.repository.JobApplicationSpecifications.hasOwner;
import static io.github.aref9571.jobtrackr.repository.JobApplicationSpecifications.hasStatus;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class JobApplicationSpecificationsTest {

    private static final Sort BY_COMPANY = Sort.by("company");
    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuuS7Ir1Dx5l6xNQ7r5BkcGcCL6kK1yJ0O";

    @Autowired
    private JobApplicationRepository repository;

    @Autowired
    private UserRepository userRepository;

    private UUID ownerId;

    @BeforeEach
    void saveApplications() {
        ownerId = userRepository.saveAndFlush(new User("ann@example.com", PASSWORD_HASH, Role.USER)).getId();
        save("ACME Corp", LocalDate.of(2026, 9, 1), ApplicationStatus.APPLIED);
        save("Acme Labs", LocalDate.of(2026, 9, 15), ApplicationStatus.INTERVIEWING);
        save("Globex", LocalDate.of(2026, 9, 30), ApplicationStatus.INTERVIEWING);
        save("Initech", LocalDate.of(2026, 10, 5), ApplicationStatus.REJECTED);
        save("50% Ventures", LocalDate.of(2026, 10, 10), ApplicationStatus.APPLIED);
        save("Data_Works", LocalDate.of(2026, 10, 12), ApplicationStatus.APPLIED);
    }

    @Test
    void findAll_noFilters_returnsAllApplications() {
        List<JobApplication> found = repository.findAll(search(null, null, null, null));

        assertThat(found).hasSize(6);
    }

    @Test
    void findAll_hasOwner_returnsOnlyThatOwnersApplications() {
        UUID otherOwnerId = userRepository.saveAndFlush(new User("bob@example.com", PASSWORD_HASH, Role.USER)).getId();
        repository.saveAndFlush(new JobApplication(
                otherOwnerId, "Globex", "Java Developer", LocalDate.of(2026, 9, 2), null, null, null));

        List<JobApplication> annApplications = repository.findAll(hasOwner(ownerId));
        List<JobApplication> bobApplications = repository.findAll(hasOwner(otherOwnerId));

        assertThat(annApplications).hasSize(6).extracting(JobApplication::getOwnerId).containsOnly(ownerId);
        assertThat(bobApplications).extracting(JobApplication::getRole).containsExactly("Java Developer");
    }

    @Test
    void findAll_statusFilter_returnsOnlyThatStatus() {
        List<JobApplication> found = repository.findAll(search(ApplicationStatus.INTERVIEWING, null, null, null), BY_COMPANY);

        assertThat(found).extracting(JobApplication::getCompany).containsExactly("Acme Labs", "Globex");
    }

    @Test
    void findAll_partOfCompanyInOtherCase_returnsMatchingCompanies() {
        List<JobApplication> found = repository.findAll(search(null, "  aCmE ", null, null), BY_COMPANY);

        assertThat(found).extracting(JobApplication::getCompany).containsExactly("ACME Corp", "Acme Labs");
    }

    @Test
    void findAll_blankCompany_ignoresCompanyFilter() {
        List<JobApplication> found = repository.findAll(search(null, "   ", null, null));

        assertThat(found).hasSize(6);
    }

    @Test
    void findAll_companyWithPercentSign_matchesPercentLiterally() {
        List<JobApplication> found = repository.findAll(search(null, "%", null, null));

        assertThat(found).extracting(JobApplication::getCompany).containsExactly("50% Ventures");
    }

    @Test
    void findAll_companyWithUnderscore_matchesUnderscoreLiterally() {
        List<JobApplication> found = repository.findAll(search(null, "_", null, null));

        assertThat(found).extracting(JobApplication::getCompany).containsExactly("Data_Works");
    }

    @Test
    void findAll_dateRange_includesBothBoundaries() {
        List<JobApplication> found = repository.findAll(
                search(null, null, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 5)), BY_COMPANY);

        assertThat(found).extracting(JobApplication::getCompany).containsExactly("Acme Labs", "Globex", "Initech");
    }

    @Test
    void findAll_onlyAppliedFrom_returnsApplicationsFromThatDate() {
        List<JobApplication> found = repository.findAll(search(null, null, LocalDate.of(2026, 10, 10), null), BY_COMPANY);

        assertThat(found).extracting(JobApplication::getCompany).containsExactly("50% Ventures", "Data_Works");
    }

    @Test
    void findAll_onlyAppliedTo_returnsApplicationsUntilThatDate() {
        List<JobApplication> found = repository.findAll(search(null, null, null, LocalDate.of(2026, 9, 1)));

        assertThat(found).extracting(JobApplication::getCompany).containsExactly("ACME Corp");
    }

    @Test
    void findAll_statusCompanyAndDateRange_returnsApplicationsMatchingAll() {
        List<JobApplication> found = repository.findAll(search(
                ApplicationStatus.INTERVIEWING, "acme", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));

        assertThat(found).extracting(JobApplication::getCompany).containsExactly("Acme Labs");
    }

    @Test
    void findAll_filtersWithoutMatch_returnsEmptyResult() {
        List<JobApplication> found = repository.findAll(search(ApplicationStatus.OFFER, "acme", null, null));

        assertThat(found).isEmpty();
    }

    @Test
    void findAll_secondPageSortedByAppliedDate_returnsThatPageAndTotals() {
        PageRequest pageRequest = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "appliedDate"));

        Page<JobApplication> page = repository.findAll(search(null, null, null, null), pageRequest);

        assertThat(page.getContent()).extracting(JobApplication::getCompany).containsExactly("Initech", "Globex");
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findAll_filterWithPaging_countsOnlyMatchingApplications() {
        PageRequest pageRequest = PageRequest.of(0, 1, BY_COMPANY);

        Page<JobApplication> page = repository.findAll(search(ApplicationStatus.APPLIED, null, null, null), pageRequest);

        assertThat(page.getContent()).extracting(JobApplication::getCompany).containsExactly("50% Ventures");
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    private static Specification<JobApplication> search(ApplicationStatus status,
                                                        String company,
                                                        LocalDate appliedFrom,
                                                        LocalDate appliedTo) {
        return Specification.allOf(
                hasStatus(status),
                companyContains(company),
                appliedOnOrAfter(appliedFrom),
                appliedOnOrBefore(appliedTo));
    }

    private void save(String company, LocalDate appliedDate, ApplicationStatus status) {
        JobApplication application = new JobApplication(
                ownerId, company, "Backend Engineer", appliedDate, null, null, null);
        if (status != ApplicationStatus.APPLIED) {
            application.changeStatus(status);
        }
        repository.saveAndFlush(application);
    }
}
