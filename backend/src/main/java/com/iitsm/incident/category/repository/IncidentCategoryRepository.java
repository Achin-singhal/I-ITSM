package com.iitsm.incident.category.repository;

import com.iitsm.incident.category.entity.IncidentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {
    List<IncidentCategory> findAllByOrderByNameAsc();
    Optional<IncidentCategory> findByKey(String key);
    boolean existsByKey(String key);
}
