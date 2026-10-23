package io.github.aref9571.jobtrackr.service;

import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ApplicationSearchCriteria;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.PageResponse;
import io.github.aref9571.jobtrackr.dto.StatusChangeResponse;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.exception.ApplicationNotFoundException;
import io.github.aref9571.jobtrackr.exception.DuplicateApplicationException;
import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;
import io.github.aref9571.jobtrackr.model.StatusChange;
import io.github.aref9571.jobtrackr.repository.JobApplicationRepository;
import io.github.aref9571.jobtrackr.repository.JobApplicationSpecifications;
import io.github.aref9571.jobtrackr.repository.StatusChangeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private final JobApplicationRepository repository;
    private final StatusChangeRepository statusChangeRepository;

    public ApplicationService(JobApplicationRepository repository, StatusChangeRepository statusChangeRepository) {
        this.repository = repository;
        this.statusChangeRepository = statusChangeRepository;
    }

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        JobApplication application = new JobApplication(
                request.company(),
                request.role(),
                request.appliedDate(),
                request.expectedSalaryCents(),
                request.salaryCurrency(),
                request.notes());
        requireNoDuplicate(application.getCompany(), application.getRole());
        JobApplication saved = repository.saveAndFlush(application);
        recordStatusChange(saved, null);
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID id) {
        return ApplicationResponse.from(findApplication(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> searchApplications(ApplicationSearchCriteria criteria, Pageable pageable) {
        Specification<JobApplication> specification = Specification.allOf(
                JobApplicationSpecifications.hasStatus(criteria.status()),
                JobApplicationSpecifications.companyContains(criteria.company()),
                JobApplicationSpecifications.appliedOnOrAfter(criteria.appliedFrom()),
                JobApplicationSpecifications.appliedOnOrBefore(criteria.appliedTo()));
        Page<ApplicationResponse> page = repository.findAll(specification, withIdTieBreaker(pageable))
                .map(ApplicationResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public ApplicationResponse updateApplication(UUID id, UpdateApplicationRequest request) {
        JobApplication application = findApplication(id);
        String company = trimOrNull(request.company());
        String role = trimOrNull(request.role());
        if (!hasCompanyAndRole(application, company, role)) {
            requireNoDuplicate(company, role);
        }
        application.updateDetails(
                company,
                role,
                request.appliedDate(),
                request.expectedSalaryCents(),
                request.salaryCurrency(),
                request.notes());
        return ApplicationResponse.from(repository.saveAndFlush(application));
    }

    @Transactional
    public ApplicationResponse changeStatus(UUID id, ChangeStatusRequest request) {
        JobApplication application = findApplication(id);
        ApplicationStatus oldStatus = application.getStatus();
        application.changeStatus(request.status());
        JobApplication saved = repository.saveAndFlush(application);
        recordStatusChange(saved, oldStatus);
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<StatusChangeResponse> getStatusHistory(UUID id) {
        requireApplicationExists(id);
        return statusChangeRepository.findByApplicationIdOrderByChangedAtAsc(id).stream()
                .map(StatusChangeResponse::from)
                .toList();
    }

    @Transactional
    public void deleteApplication(UUID id) {
        JobApplication application = findApplication(id);
        repository.delete(application);
    }

    private JobApplication findApplication(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("No application found with id: " + id));
    }

    private void requireApplicationExists(UUID id) {
        if (!repository.existsById(id)) {
            throw new ApplicationNotFoundException("No application found with id: " + id);
        }
    }

    private void recordStatusChange(JobApplication application, ApplicationStatus oldStatus) {
        statusChangeRepository.save(new StatusChange(application, oldStatus, application.getStatus()));
    }

    private void requireNoDuplicate(String company, String role) {
        if (repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase(company, role)) {
            throw new DuplicateApplicationException("Application for " + role + " at " + company + " already exists");
        }
    }

    private static Pageable withIdTieBreaker(Pageable pageable) {
        Sort sort = pageable.getSort().and(Sort.by("id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static boolean hasCompanyAndRole(JobApplication application, String company, String role) {
        return application.getCompany().equalsIgnoreCase(company) && application.getRole().equalsIgnoreCase(role);
    }

    private static String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }
}
