package io.github.aref9571.jobtrackr.dto;

import io.github.aref9571.jobtrackr.model.ApplicationStatus;

public record StatusCount(ApplicationStatus status, long count) {
}
