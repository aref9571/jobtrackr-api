package io.github.aref9571.jobtrackr.dto;

import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.JobApplication;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String company,
        String role,
        ApplicationStatus status,
        LocalDate appliedDate,
        Long expectedSalaryCents,
        String salaryCurrency,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static ApplicationResponse from(JobApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getCompany(),
                application.getRole(),
                application.getStatus(),
                application.getAppliedDate(),
                application.getExpectedSalaryCents(),
                application.getSalaryCurrency(),
                application.getNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
