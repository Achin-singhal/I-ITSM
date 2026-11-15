package com.iitsm.audit.repository;

import com.iitsm.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
