package com.iitsm.sla.repository;
import com.iitsm.sla.entity.SlaTracking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
public interface SlaTrackingRepository extends JpaRepository<SlaTracking,Long>{
 Optional<SlaTracking> findByIncidentId(Long incidentId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select tracking from SlaTracking tracking where tracking.incident.id = :incidentId")
 Optional<SlaTracking> findByIncidentIdForUpdate(@Param("incidentId") Long incidentId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select tracking from SlaTracking tracking where tracking.resolvedAt is null order by tracking.id")
 List<SlaTracking> lockUnresolved();
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select tracking from SlaTracking tracking order by tracking.id")
 List<SlaTracking> lockAllForUpdate();

 @Query("select count(t), sum(case when t.responseWarningSent = true then 1 else 0 end), sum(case when t.resolutionWarningSent = true then 1 else 0 end), sum(case when t.responseViolationRecorded = true then 1 else 0 end), sum(case when t.resolutionViolationRecorded = true then 1 else 0 end) from SlaTracking t where (cast(:from as Instant) is null or t.incident.createdAt >= :from) and (cast(:to as Instant) is null or t.incident.createdAt < :to)")
 Object[] aggregateReportData(@Param("from") Instant from,@Param("to") Instant to);

 @EntityGraph(attributePaths={"incident","policy"})
 @Query(value="select t from SlaTracking t where (cast(:from as Instant) is null or t.incident.createdAt >= :from) and (cast(:to as Instant) is null or t.incident.createdAt < :to)",
        countQuery="select count(t) from SlaTracking t where (cast(:from as Instant) is null or t.incident.createdAt >= :from) and (cast(:to as Instant) is null or t.incident.createdAt < :to)")
 Page<SlaTracking> findReportPage(@Param("from") Instant from,@Param("to") Instant to,Pageable pageable);
}
