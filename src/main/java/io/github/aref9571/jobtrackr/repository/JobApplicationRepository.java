package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID>,
        JpaSpecificationExecutor<JobApplication> {

    boolean existsByCompanyIgnoreCaseAndRoleIgnoreCase(String company, String role);
}
