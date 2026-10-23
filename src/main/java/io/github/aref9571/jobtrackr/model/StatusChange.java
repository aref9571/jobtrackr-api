package io.github.aref9571.jobtrackr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "application_status_changes")
public class StatusChange {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, updatable = false)
    private ApplicationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ApplicationStatus newStatus;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    protected StatusChange() {
    }

    public StatusChange(JobApplication application, ApplicationStatus oldStatus, ApplicationStatus newStatus) {
        if (application == null) {
            throw new IllegalArgumentException("application must not be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus must not be null");
        }
        this.id = UUID.randomUUID();
        this.application = application;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public UUID getId() {
        return id;
    }

    public JobApplication getApplication() {
        return application;
    }

    public ApplicationStatus getOldStatus() {
        return oldStatus;
    }

    public ApplicationStatus getNewStatus() {
        return newStatus;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
