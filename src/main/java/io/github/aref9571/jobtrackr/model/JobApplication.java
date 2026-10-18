package io.github.aref9571.jobtrackr.model;

import io.github.aref9571.jobtrackr.exception.InvalidApplicationStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    private static final int MAX_COMPANY_LENGTH = 100;
    private static final int MAX_ROLE_LENGTH = 100;
    private static final int MAX_NOTES_LENGTH = 1000;

    @Id
    private UUID id;

    @Column(nullable = false, length = MAX_COMPANY_LENGTH)
    private String company;

    @Column(nullable = false, length = MAX_ROLE_LENGTH)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDate appliedDate;

    private Long expectedSalaryCents;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String salaryCurrency;

    @Column(length = MAX_NOTES_LENGTH)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected JobApplication() {
    }

    public JobApplication(String company,
                          String role,
                          LocalDate appliedDate,
                          Long expectedSalaryCents,
                          String salaryCurrency,
                          String notes) {
        this.id = UUID.randomUUID();
        this.status = ApplicationStatus.APPLIED;
        setDetails(company, role, appliedDate, expectedSalaryCents, salaryCurrency, notes);
    }

    public void updateDetails(String company,
                              String role,
                              LocalDate appliedDate,
                              Long expectedSalaryCents,
                              String salaryCurrency,
                              String notes) {
        setDetails(company, role, appliedDate, expectedSalaryCents, salaryCurrency, notes);
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

    @PrePersist
    void onCreate() {
        createdAt = currentTime();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = currentTime();
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static Instant currentTime() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private void setDetails(String company,
                            String role,
                            LocalDate appliedDate,
                            Long expectedSalaryCents,
                            String salaryCurrency,
                            String notes) {
        String validCompany = requireText(company, "company", MAX_COMPANY_LENGTH);
        String validRole = requireText(role, "role", MAX_ROLE_LENGTH);
        LocalDate validAppliedDate = requireValidAppliedDate(appliedDate);
        validateSalary(expectedSalaryCents, salaryCurrency);
        String validNotes = requireValidNotes(notes);

        this.company = validCompany;
        this.role = validRole;
        this.appliedDate = validAppliedDate;
        this.expectedSalaryCents = expectedSalaryCents;
        this.salaryCurrency = salaryCurrency;
        this.notes = validNotes;
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
