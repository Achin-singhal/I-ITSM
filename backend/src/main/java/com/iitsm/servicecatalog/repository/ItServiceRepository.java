package com.iitsm.servicecatalog.repository;

import com.iitsm.servicecatalog.entity.ItService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItServiceRepository extends JpaRepository<ItService, Long> {
    List<ItService> findAllByOrderByNameAsc();
    List<ItService> findAllByActiveTrueOrderByNameAsc();
    Optional<ItService> findByServiceKey(String serviceKey);
    boolean existsByServiceKey(String serviceKey);
}
