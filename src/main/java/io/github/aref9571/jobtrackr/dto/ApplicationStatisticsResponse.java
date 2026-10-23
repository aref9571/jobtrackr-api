package io.github.aref9571.jobtrackr.dto;

import io.github.aref9571.jobtrackr.model.ApplicationStatus;

import java.util.List;
import java.util.Map;

public record ApplicationStatisticsResponse(
        long total,
        long active,
        Map<ApplicationStatus, Long> byStatus,
        List<CompanyCount> topCompanies) {
}
