package io.github.aref9571.jobtrackr.dto;

import io.github.aref9571.jobtrackr.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull ApplicationStatus status) {
}
