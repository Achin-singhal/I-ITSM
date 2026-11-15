package com.iitsm.sla.repository;
import com.iitsm.sla.entity.SlaHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SlaHistoryRepository extends JpaRepository<SlaHistoryEntry,Long>{List<SlaHistoryEntry> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);}
