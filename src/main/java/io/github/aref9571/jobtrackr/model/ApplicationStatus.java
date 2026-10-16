package io.github.aref9571.jobtrackr.model;

public enum ApplicationStatus {
    APPLIED,
    INTERVIEWING,
    OFFER,
    REJECTED;

    public boolean isActive() {
        return this == APPLIED || this == INTERVIEWING;
    }

    public boolean isFinal() {
        return this == OFFER || this == REJECTED;
    }

    public boolean canMoveTo(ApplicationStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case APPLIED -> next == INTERVIEWING || next == REJECTED;
            case INTERVIEWING -> next == OFFER || next == REJECTED;
            case OFFER, REJECTED -> false;
        };
    }
}
