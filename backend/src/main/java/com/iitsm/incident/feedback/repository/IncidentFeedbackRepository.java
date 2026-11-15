package com.iitsm.incident.feedback.repository;

import com.iitsm.incident.feedback.entity.IncidentFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IncidentFeedbackRepository extends JpaRepository<IncidentFeedback, Long> {
    Optional<IncidentFeedback> findByIncidentId(Long incidentId);
    boolean existsByIncidentId(Long incidentId);
    Page<IncidentFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
