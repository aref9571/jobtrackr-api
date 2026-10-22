package io.github.aref9571.jobtrackr.dto;

import io.github.aref9571.jobtrackr.model.ApplicationStatus;

import java.time.LocalDate;

public record ApplicationSearchCriteria(
        ApplicationStatus status,
        String company,
        LocalDate appliedFrom,
        LocalDate appliedTo) {
}
