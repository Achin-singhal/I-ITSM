package com.iitsm.incident.repository;

import com.iitsm.incident.entity.Incident;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import com.iitsm.sla.entity.SlaStatus;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByTicketNumber(String ticketNumber);

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByPriority(IncidentPriority priority);

    long countByStatus(IncidentStatus status);

    List<Incident> findByReporterId(Long reporterId);
    List<Incident> findByAssigneeIdOrAssigneeIsNullOrderByCreatedAtDesc(Long assigneeId);

    long countByAssigneeIdAndStatusIn(Long assigneeId, Collection<IncidentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select incident from Incident incident where incident.id = :id")
    Optional<Incident> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"reporter", "assignee"})
    @Query(value = "select i from Incident i left join SlaTracking s on s.incident.id = i.id where " +
            "(:incidentId is null or i.id = :incidentId) and " +
            "(cast(:query as string) is null or lower(i.ticketNumber) like lower(concat('%', cast(:query as string), '%')) or " +
            "lower(i.title) like lower(concat('%', cast(:query as string), '%')) or lower(i.description) like lower(concat('%', cast(:query as string), '%'))) and " +
            "(:status is null or i.status = :status) and (:priority is null or i.priority = :priority) and " +
            "(cast(:category as string) is null or lower(i.category) = lower(cast(:category as string))) and (:assigneeId is null or i.assignee.id = :assigneeId) and " +
            "(:reporterId is null or i.reporter.id = :reporterId) and " +
            "(:technicianScopeId is null or i.assignee is null or i.assignee.id = :technicianScopeId) and " +
            "(cast(:createdFrom as Instant) is null or i.createdAt >= :createdFrom) and (cast(:createdTo as Instant) is null or i.createdAt < :createdTo) and " +
            "(:slaStatus is null or s.status = :slaStatus)",
            countQuery = "select count(i) from Incident i left join SlaTracking s on s.incident.id = i.id where " +
            "(:incidentId is null or i.id = :incidentId) and " +
            "(cast(:query as string) is null or lower(i.ticketNumber) like lower(concat('%', cast(:query as string), '%')) or " +
            "lower(i.title) like lower(concat('%', cast(:query as string), '%')) or lower(i.description) like lower(concat('%', cast(:query as string), '%'))) and " +
            "(:status is null or i.status = :status) and (:priority is null or i.priority = :priority) and " +
            "(cast(:category as string) is null or lower(i.category) = lower(cast(:category as string))) and (:assigneeId is null or i.assignee.id = :assigneeId) and " +
            "(:reporterId is null or i.reporter.id = :reporterId) and " +
            "(:technicianScopeId is null or i.assignee is null or i.assignee.id = :technicianScopeId) and " +
            "(cast(:createdFrom as Instant) is null or i.createdAt >= :createdFrom) and (cast(:createdTo as Instant) is null or i.createdAt < :createdTo) and " +
            "(:slaStatus is null or s.status = :slaStatus)")
    Page<Incident> searchIncidents(@Param("incidentId") Long incidentId, @Param("query") String query,
            @Param("status") IncidentStatus status, @Param("priority") IncidentPriority priority,
            @Param("category") String category, @Param("assigneeId") Long assigneeId,
            @Param("reporterId") Long reporterId,
            @Param("technicianScopeId") Long technicianScopeId,
            @Param("createdFrom") Instant createdFrom, @Param("createdTo") Instant createdTo,
            @Param("slaStatus") SlaStatus slaStatus, Pageable pageable);

    @Query("select count(i) from Incident i where (cast(:from as Instant) is null or i.createdAt >= :from) and (cast(:to as Instant) is null or i.createdAt < :to)")
    long countCreatedInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(i) from Incident i where i.status in :statuses and (cast(:from as Instant) is null or i.createdAt >= :from) and (cast(:to as Instant) is null or i.createdAt < :to)")
    long countCreatedWithStatuses(@Param("statuses") Collection<IncidentStatus> statuses, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(i) from Incident i where i.priority in :priorities and (cast(:from as Instant) is null or i.createdAt >= :from) and (cast(:to as Instant) is null or i.createdAt < :to)")
    long countCreatedWithPriorities(@Param("priorities") Collection<IncidentPriority> priorities, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select i.status, count(i) from Incident i where (cast(:from as Instant) is null or i.createdAt >= :from) and (cast(:to as Instant) is null or i.createdAt < :to) group by i.status")
    List<Object[]> aggregateStatus(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select i.priority, count(i) from Incident i where (cast(:from as Instant) is null or i.createdAt >= :from) and (cast(:to as Instant) is null or i.createdAt < :to) group by i.priority")
    List<Object[]> aggregatePriority(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select i.category, count(i) from Incident i where (cast(:from as Instant) is null or i.createdAt >= :from) and (cast(:to as Instant) is null or i.createdAt < :to) group by i.category order by i.category")
    List<Object[]> aggregateCategory(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "select cast(created_at as date), count(*) from incidents where (cast(:from as timestamp with time zone) is null or created_at >= :from) and (cast(:to as timestamp with time zone) is null or created_at < :to) group by cast(created_at as date) order by cast(created_at as date)", nativeQuery = true)
    List<Object[]> aggregateCreatedByDate(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value="select avg(extract(epoch from (resolved_at - created_at))) from incidents where resolved_at is not null and resolved_at >= created_at and (cast(:from as timestamp with time zone) is null or created_at >= :from) and (cast(:to as timestamp with time zone) is null or created_at < :to)",nativeQuery=true)
    Object averageResolutionSeconds(@Param("from") Instant from,@Param("to") Instant to);
}
