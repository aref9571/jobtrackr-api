package io.github.aref9571.jobtrackr.dto;

import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import io.github.aref9571.jobtrackr.model.StatusChange;

import java.time.Instant;

public record StatusChangeResponse(
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        Instant changedAt) {

    public static StatusChangeResponse from(StatusChange statusChange) {
        return new StatusChangeResponse(
                statusChange.getOldStatus(),
                statusChange.getNewStatus(),
                statusChange.getChangedAt());
    }
}
