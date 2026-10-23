package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.model.StatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StatusChangeRepository extends JpaRepository<StatusChange, UUID> {

    List<StatusChange> findByApplicationIdOrderByChangedAtAsc(UUID applicationId);
}
