package io.github.aref9571.jobtrackr.dto;

import java.time.LocalDate;

public record UpdateApplicationRequest(
        String company,
        String role,
        LocalDate appliedDate,
        Long expectedSalaryCents,
        String salaryCurrency,
        String notes) {
}
