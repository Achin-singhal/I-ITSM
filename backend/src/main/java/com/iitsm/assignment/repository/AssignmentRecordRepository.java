package com.iitsm.assignment.repository;

import com.iitsm.assignment.entity.AssignmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRecordRepository extends JpaRepository<AssignmentRecord,Long> {
    List<AssignmentRecord> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
