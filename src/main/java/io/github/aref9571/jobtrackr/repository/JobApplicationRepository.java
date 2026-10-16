package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    boolean existsByCompanyIgnoreCaseAndRoleIgnoreCase(String company, String role);
}
