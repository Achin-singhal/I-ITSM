package com.iitsm.sla.repository;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.sla.entity.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy,Long>{Optional<SlaPolicy> findByPriorityAndActiveTrue(IncidentPriority priority);}
