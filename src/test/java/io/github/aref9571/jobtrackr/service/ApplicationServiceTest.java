package io.github.aref9571.jobtrackr.service;

import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ApplicationSearchCriteria;
import io.github.aref9571.jobtrackr.dto.ApplicationStatisticsResponse;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CompanyCount;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.PageResponse;
import io.github.aref9571.jobtrackr.dto.StatusChangeResponse;
import io.github.aref9571.jobtrackr.dto.StatusCount;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.exception.ApplicationNotFoundException;
import io.github.aref9571.jobtrackr.exception.DuplicateApplicationException;
import io.github.aref9571.jobtrackr.exception.InvalidApplicationStateException;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;
import io.github.aref9571.jobtrackr.model.StatusChange;
import io.github.aref9571.jobtrackr.repository.JobApplicationRepository;
import io.github.aref9571.jobtrackr.repository.StatusChangeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final LocalDate APPLIED_DATE = LocalDate.of(2026, 10, 1);
    private static final ApplicationSearchCriteria NO_FILTERS = new ApplicationSearchCriteria(null, null, null, null);

    @Mock
    private JobApplicationRepository repository;

    @Mock
    private StatusChangeRepository statusChangeRepository;

    @InjectMocks
    private ApplicationService service;

    @Test
    void createApplication_newCompanyAndRole_savesAndReturnsApplication() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, "EUR", "Referred by a friend");
        when(repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("ACME Corp", "Backend Engineer")).thenReturn(false);
        when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = service.createApplication(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.company()).isEqualTo("ACME Corp");
        assertThat(response.role()).isEqualTo("Backend Engineer");
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.appliedDate()).isEqualTo(APPLIED_DATE);
        assertThat(response.expectedSalaryCents()).isEqualTo(5_000_000L);
        assertThat(response.salaryCurrency()).isEqualTo("EUR");
        assertThat(response.notes()).isEqualTo("Referred by a friend");
        verify(repository).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void createApplication_companyAndRoleWithSpaces_checksDuplicateWithTrimmedValues() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "  ACME Corp ", " Backend Engineer ", APPLIED_DATE, null, null, null);
        when(repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("ACME Corp", "Backend Engineer")).thenReturn(false);
        when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = service.createApplication(request);

        assertThat(response.company()).isEqualTo("ACME Corp");
        assertThat(response.role()).isEqualTo("Backend Engineer");
    }

    @Test
    void createApplication_sameCompanyAndRoleExists_throwsException() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "acme corp", "backend engineer", APPLIED_DATE, null, null, null);
        when(repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("acme corp", "backend engineer")).thenReturn(true);

        assertThatThrownBy(() -> service.createApplication(request))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessage("Application for backend engineer at acme corp already exists");
        verify(repository, never()).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void createApplication_invalidInput_throwsExceptionWithoutRepositoryCalls() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                " ", "Backend Engineer", APPLIED_DATE, null, null, null);

        assertThatThrownBy(() -> service.createApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("company must not be blank");
        verify(repository, never()).existsByCompanyIgnoreCaseAndRoleIgnoreCase(anyString(), anyString());
        verify(repository, never()).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void getApplication_existingId_returnsApplication() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        ApplicationResponse response = service.getApplication(application.getId());

        assertThat(response.id()).isEqualTo(application.getId());
        assertThat(response.company()).isEqualTo("ACME Corp");
    }

    @Test
    void getApplication_unknownId_throwsException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplication(id))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage("No application found with id: " + id);
    }

    @Test
    void searchApplications_twoApplications_returnsPageWithBoth() {
        JobApplication first = application("ACME Corp", "Backend Engineer");
        JobApplication second = application("Globex", "Java Developer");
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));

        PageResponse<ApplicationResponse> response = service.searchApplications(NO_FILTERS, pageable);

        assertThat(response.content())
                .extracting(ApplicationResponse::id)
                .containsExactly(first.getId(), second.getId());
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void searchApplications_noApplications_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<ApplicationResponse> response = service.searchApplications(NO_FILTERS, pageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void searchApplications_sortedByCompany_addsIdAsLastSortOrder() {
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "company"));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 5));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        service.searchApplications(NO_FILTERS, pageable);

        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort()).containsExactly(Sort.Order.desc("company"), Sort.Order.asc("id"));
    }

    @Test
    void updateApplication_newCompany_savesNewDetails() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        UpdateApplicationRequest request = new UpdateApplicationRequest(
                "Globex", "Backend Engineer", LocalDate.of(2026, 9, 15), 6_000_000L, "USD", "Second round");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));
        when(repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("Globex", "Backend Engineer")).thenReturn(false);
        when(repository.saveAndFlush(application)).thenReturn(application);

        ApplicationResponse response = service.updateApplication(application.getId(), request);

        assertThat(response.company()).isEqualTo("Globex");
        assertThat(response.role()).isEqualTo("Backend Engineer");
        assertThat(response.appliedDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(response.expectedSalaryCents()).isEqualTo(6_000_000L);
        assertThat(response.salaryCurrency()).isEqualTo("USD");
        assertThat(response.notes()).isEqualTo("Second round");
        verify(repository).saveAndFlush(application);
    }

    @Test
    void updateApplication_newRole_checksDuplicateAndSaves() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        UpdateApplicationRequest request = new UpdateApplicationRequest(
                "ACME Corp", "Java Developer", APPLIED_DATE, null, null, null);
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));
        when(repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("ACME Corp", "Java Developer")).thenReturn(false);
        when(repository.saveAndFlush(application)).thenReturn(application);

        ApplicationResponse response = service.updateApplication(application.getId(), request);

        assertThat(response.role()).isEqualTo("Java Developer");
        verify(repository).existsByCompanyIgnoreCaseAndRoleIgnoreCase("ACME Corp", "Java Developer");
    }

    @Test
    void updateApplication_sameCompanyAndRoleInOtherCaseAndSpaces_skipsDuplicateCheck() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        UpdateApplicationRequest request = new UpdateApplicationRequest(
                " acme corp ", "BACKEND ENGINEER", APPLIED_DATE, null, null, "Phone call booked");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));
        when(repository.saveAndFlush(application)).thenReturn(application);

        ApplicationResponse response = service.updateApplication(application.getId(), request);

        assertThat(response.company()).isEqualTo("acme corp");
        assertThat(response.role()).isEqualTo("BACKEND ENGINEER");
        assertThat(response.notes()).isEqualTo("Phone call booked");
        verify(repository, never()).existsByCompanyIgnoreCaseAndRoleIgnoreCase(anyString(), anyString());
    }

    @Test
    void updateApplication_companyAndRoleOfAnotherApplication_throwsExceptionAndKeepsDetails() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        UpdateApplicationRequest request = new UpdateApplicationRequest(
                "Globex", "Java Developer", APPLIED_DATE, null, null, null);
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));
        when(repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("Globex", "Java Developer")).thenReturn(true);

        assertThatThrownBy(() -> service.updateApplication(application.getId(), request))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessage("Application for Java Developer at Globex already exists");
        assertThat(application.getCompany()).isEqualTo("ACME Corp");
        assertThat(application.getRole()).isEqualTo("Backend Engineer");
        verify(repository, never()).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void updateApplication_invalidInput_throwsExceptionWithoutSaving() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        UpdateApplicationRequest request = new UpdateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, 5_000_000L, null, null);
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.updateApplication(application.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expectedSalaryCents and salaryCurrency must both be present or both be absent");
        verify(repository, never()).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void updateApplication_unknownId_throwsException() {
        UUID id = UUID.randomUUID();
        UpdateApplicationRequest request = new UpdateApplicationRequest(
                "Globex", "Java Developer", APPLIED_DATE, null, null, null);
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateApplication(id, request))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage("No application found with id: " + id);
        verify(repository, never()).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void changeStatus_allowedChange_savesNewStatus() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));
        when(repository.saveAndFlush(application)).thenReturn(application);

        ApplicationResponse response = service.changeStatus(
                application.getId(), new ChangeStatusRequest(ApplicationStatus.INTERVIEWING));

        assertThat(response.status()).isEqualTo(ApplicationStatus.INTERVIEWING);
        verify(repository).saveAndFlush(application);
    }

    @Test
    void changeStatus_fromAppliedToOffer_throwsExceptionWithoutSaving() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.changeStatus(
                application.getId(), new ChangeStatusRequest(ApplicationStatus.OFFER)))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessage("Cannot move from APPLIED to OFFER");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        verify(repository, never()).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void changeStatus_fromFinalStatus_throwsExceptionWithoutSaving() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        application.changeStatus(ApplicationStatus.REJECTED);
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.changeStatus(
                application.getId(), new ChangeStatusRequest(ApplicationStatus.INTERVIEWING)))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessage("Cannot change status: application is already in final state REJECTED");
        verify(repository, never()).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void changeStatus_nullStatus_throwsExceptionWithoutSaving() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.changeStatus(application.getId(), new ChangeStatusRequest(null)))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessage("new status must not be null");
        verify(repository, never()).saveAndFlush(any(JobApplication.class));
    }

    @Test
    void changeStatus_unknownId_throwsException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(id, new ChangeStatusRequest(ApplicationStatus.INTERVIEWING)))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage("No application found with id: " + id);
    }

    @Test
    void deleteApplication_existingId_deletesApplication() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        service.deleteApplication(application.getId());

        verify(repository).delete(application);
    }

    @Test
    void deleteApplication_unknownId_throwsException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteApplication(id))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage("No application found with id: " + id);
        verify(repository, never()).delete(any(JobApplication.class));
    }

    @Test
    void createApplication_newApplication_recordsFirstStatusChange() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null);
        when(repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("ACME Corp", "Backend Engineer")).thenReturn(false);
        when(repository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = service.createApplication(request);

        StatusChange recorded = captureRecordedStatusChange();
        assertThat(recorded.getApplication().getId()).isEqualTo(response.id());
        assertThat(recorded.getOldStatus()).isNull();
        assertThat(recorded.getNewStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void createApplication_duplicate_recordsNoStatusChange() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, null);
        when(repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase("ACME Corp", "Backend Engineer")).thenReturn(true);

        assertThatThrownBy(() -> service.createApplication(request))
                .isInstanceOf(DuplicateApplicationException.class);
        verify(statusChangeRepository, never()).save(any(StatusChange.class));
    }

    @Test
    void changeStatus_allowedChange_recordsOldAndNewStatus() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));
        when(repository.saveAndFlush(application)).thenReturn(application);

        service.changeStatus(application.getId(), new ChangeStatusRequest(ApplicationStatus.INTERVIEWING));

        StatusChange recorded = captureRecordedStatusChange();
        assertThat(recorded.getApplication()).isSameAs(application);
        assertThat(recorded.getOldStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(recorded.getNewStatus()).isEqualTo(ApplicationStatus.INTERVIEWING);
    }

    @Test
    void changeStatus_forbiddenChange_recordsNoStatusChange() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.changeStatus(
                application.getId(), new ChangeStatusRequest(ApplicationStatus.OFFER)))
                .isInstanceOf(InvalidApplicationStateException.class);
        verify(statusChangeRepository, never()).save(any(StatusChange.class));
    }

    @Test
    void updateApplication_detailsChanged_recordsNoStatusChange() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        UpdateApplicationRequest request = new UpdateApplicationRequest(
                "ACME Corp", "Backend Engineer", APPLIED_DATE, null, null, "Phone call booked");
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));
        when(repository.saveAndFlush(application)).thenReturn(application);

        service.updateApplication(application.getId(), request);

        verify(statusChangeRepository, never()).save(any(StatusChange.class));
    }

    @Test
    void getStatusHistory_existingApplication_returnsEntriesInRepositoryOrder() {
        JobApplication application = application("ACME Corp", "Backend Engineer");
        StatusChange first = new StatusChange(application, null, ApplicationStatus.APPLIED);
        StatusChange second = new StatusChange(application, ApplicationStatus.APPLIED, ApplicationStatus.REJECTED);
        when(repository.existsById(application.getId())).thenReturn(true);
        when(statusChangeRepository.findByApplicationIdOrderByChangedAtAsc(application.getId()))
                .thenReturn(List.of(first, second));

        List<StatusChangeResponse> history = service.getStatusHistory(application.getId());

        assertThat(history)
                .extracting(StatusChangeResponse::oldStatus, StatusChangeResponse::newStatus,
                        StatusChangeResponse::changedAt)
                .containsExactly(
                        tuple(null, ApplicationStatus.APPLIED, first.getChangedAt()),
                        tuple(ApplicationStatus.APPLIED, ApplicationStatus.REJECTED, second.getChangedAt()));
    }

    @Test
    void getStatusHistory_unknownId_throwsExceptionWithoutLoadingHistory() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.getStatusHistory(id))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage("No application found with id: " + id);
        verify(statusChangeRepository, never()).findByApplicationIdOrderByChangedAtAsc(any(UUID.class));
    }

    @Test
    void getStatistics_someStatusesMissing_returnsZeroForThemAndTotals() {
        List<CompanyCount> topCompanies = List.of(new CompanyCount("acme corp", 2), new CompanyCount("globex", 1));
        when(repository.countPerStatus()).thenReturn(List.of(
                new StatusCount(ApplicationStatus.APPLIED, 3),
                new StatusCount(ApplicationStatus.INTERVIEWING, 2),
                new StatusCount(ApplicationStatus.REJECTED, 1)));
        when(repository.findTopCompanies(Limit.of(5))).thenReturn(topCompanies);

        ApplicationStatisticsResponse statistics = service.getStatistics();

        assertThat(statistics.total()).isEqualTo(6);
        assertThat(statistics.active()).isEqualTo(5);
        assertThat(statistics.byStatus()).containsExactly(
                entry(ApplicationStatus.APPLIED, 3L),
                entry(ApplicationStatus.INTERVIEWING, 2L),
                entry(ApplicationStatus.OFFER, 0L),
                entry(ApplicationStatus.REJECTED, 1L));
        assertThat(statistics.topCompanies()).isEqualTo(topCompanies);
    }

    @Test
    void getStatistics_noApplications_returnsZeroForEveryStatus() {
        when(repository.countPerStatus()).thenReturn(List.of());
        when(repository.findTopCompanies(Limit.of(5))).thenReturn(List.of());

        ApplicationStatisticsResponse statistics = service.getStatistics();

        assertThat(statistics.total()).isZero();
        assertThat(statistics.active()).isZero();
        assertThat(statistics.byStatus())
                .containsOnlyKeys(ApplicationStatus.values())
                .allSatisfy((status, count) -> assertThat(count).isZero());
        assertThat(statistics.topCompanies()).isEmpty();
    }

    private StatusChange captureRecordedStatusChange() {
        ArgumentCaptor<StatusChange> statusChangeCaptor = ArgumentCaptor.forClass(StatusChange.class);
        verify(statusChangeRepository).save(statusChangeCaptor.capture());
        return statusChangeCaptor.getValue();
    }

    private static JobApplication application(String company, String role) {
        return new JobApplication(company, role, APPLIED_DATE, null, null, null);
    }
}
