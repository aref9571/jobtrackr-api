package io.github.aref9571.jobtrackr.model;

import io.github.aref9571.jobtrackr.exception.InvalidApplicationStateException;

import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

public class JobApplication {

    private static final int MAX_COMPANY_LENGTH = 100;
    private static final int MAX_ROLE_LENGTH = 100;
    private static final int MAX_NOTES_LENGTH = 1000;

    private final UUID id;
    private final String company;
    private final String role;
    private ApplicationStatus status;
    private final LocalDate appliedDate;
    private final Long expectedSalaryCents;
    private final String salaryCurrency;
    private final String notes;

    public JobApplication(String company,
                          String role,
                          LocalDate appliedDate,
                          Long expectedSalaryCents,
                          String salaryCurrency,
                          String notes) {
        this.id = UUID.randomUUID();
        this.company = requireText(company, "company", MAX_COMPANY_LENGTH);
        this.role = requireText(role, "role", MAX_ROLE_LENGTH);
        this.status = ApplicationStatus.APPLIED;
        this.appliedDate = requireValidAppliedDate(appliedDate);
        validateSalary(expectedSalaryCents, salaryCurrency);
        this.expectedSalaryCents = expectedSalaryCents;
        this.salaryCurrency = salaryCurrency;
        this.notes = requireValidNotes(notes);
    }

    public void changeStatus(ApplicationStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidApplicationStateException("new status must not be null");
        }
        if (status.isFinal()) {
            throw new InvalidApplicationStateException(
                    "Cannot change status: application is already in final state " + status);
        }
        if (!status.canMoveTo(newStatus)) {
            throw new InvalidApplicationStateException("Cannot move from " + status + " to " + newStatus);
        }
        status = newStatus;
    }

    public UUID getId() {
        return id;
    }

    public String getCompany() {
        return company;
    }

    public String getRole() {
        return role;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public Long getExpectedSalaryCents() {
        return expectedSalaryCents;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public String getNotes() {
        return notes;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    private static LocalDate requireValidAppliedDate(LocalDate appliedDate) {
        if (appliedDate == null) {
            throw new IllegalArgumentException("appliedDate must not be null");
        }
        if (appliedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("appliedDate must not be in the future");
        }
        return appliedDate;
    }

    private static void validateSalary(Long amountInCents, String currency) {
        if (amountInCents == null && currency == null) {
            return;
        }
        if (amountInCents == null || currency == null) {
            throw new IllegalArgumentException("expectedSalaryCents and salaryCurrency must both be present or both be absent");
        }
        if (amountInCents < 0) {
            throw new IllegalArgumentException("expectedSalaryCents must not be negative");
        }
        if (!isCurrencyCode(currency)) {
            throw new IllegalArgumentException("salaryCurrency must be a three-letter upper-case ISO 4217 code");
        }
    }

    private static boolean isCurrencyCode(String currency) {
        if (!currency.matches("[A-Z]{3}")) {
            return false;
        }
        try {
            Currency.getInstance(currency);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String requireValidNotes(String notes) {
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("notes must be at most " + MAX_NOTES_LENGTH + " characters");
        }
        return notes;
    }
}
