package com.iitsm.incident.repository;
import com.iitsm.incident.entity.IncidentActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface IncidentActivityRepository extends JpaRepository<IncidentActivity,Long>{List<IncidentActivity> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);}
