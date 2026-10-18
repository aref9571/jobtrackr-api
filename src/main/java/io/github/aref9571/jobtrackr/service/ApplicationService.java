package io.github.aref9571.jobtrackr.service;

import io.github.aref9571.jobtrackr.dto.ApplicationResponse;
import io.github.aref9571.jobtrackr.dto.ChangeStatusRequest;
import io.github.aref9571.jobtrackr.dto.CreateApplicationRequest;
import io.github.aref9571.jobtrackr.dto.UpdateApplicationRequest;
import io.github.aref9571.jobtrackr.exception.ApplicationNotFoundException;
import io.github.aref9571.jobtrackr.exception.DuplicateApplicationException;
import io.github.aref9571.jobtrackr.model.JobApplication;
import io.github.aref9571.jobtrackr.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private final JobApplicationRepository repository;

    public ApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
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
        return ApplicationResponse.from(repository.saveAndFlush(application));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID id) {
        return ApplicationResponse.from(findApplication(id));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications() {
        return repository.findAll().stream()
                .map(ApplicationResponse::from)
                .toList();
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
        application.changeStatus(request.status());
        return ApplicationResponse.from(repository.saveAndFlush(application));
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

    private void requireNoDuplicate(String company, String role) {
        if (repository.existsByCompanyIgnoreCaseAndRoleIgnoreCase(company, role)) {
            throw new DuplicateApplicationException("Application for " + role + " at " + company + " already exists");
        }
    }

    private static boolean hasCompanyAndRole(JobApplication application, String company, String role) {
        return application.getCompany().equalsIgnoreCase(company) && application.getRole().equalsIgnoreCase(role);
    }

    private static String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }
}
