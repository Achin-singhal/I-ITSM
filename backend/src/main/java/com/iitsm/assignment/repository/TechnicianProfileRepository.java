package com.iitsm.assignment.repository;

import com.iitsm.assignment.entity.TechnicianProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface TechnicianProfileRepository extends JpaRepository<TechnicianProfile,Long> {
    Optional<TechnicianProfile> findByUserId(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from TechnicianProfile profile where profile.user.id = :userId")
    Optional<TechnicianProfile> findByUserIdForUpdate(@Param("userId") Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from TechnicianProfile profile order by profile.id")
    List<TechnicianProfile> findAllForAssignment();
    List<TechnicianProfile> findAllByAvailableTrue();

    @Query(value="""
        select u.id as technician_id, u.full_name as technician_name, u.email as technician_email,
               p.available as available, p.max_active_workload as max_active_workload,
               count(i.id) as assigned_incident_count,
               (select count(*) from incidents active_i where active_i.assignee_id=u.id
                 and active_i.status in ('NEW','OPEN','CLASSIFIED','ASSIGNED','ACKNOWLEDGED','IN_PROGRESS')) as active_workload,
               sum(case when i.status in ('RESOLVED','VERIFIED','CLOSED') then 1 else 0 end) as resolved_incident_count,
               avg(case when i.resolved_at is not null and i.resolved_at >= i.created_at
                        then extract(epoch from (i.resolved_at - i.created_at)) else null end) as average_resolution_seconds
        from technician_profiles p join users u on u.id=p.user_id
        left join incidents i on i.assignee_id=u.id and (cast(:from as timestamp with time zone) is null or i.created_at >= :from) and (cast(:to as timestamp with time zone) is null or i.created_at < :to)
        where u.active=true and u.role in ('TECHNICIAN','AGENT') and (:technicianId is null or u.id=:technicianId)
        group by u.id,u.full_name,u.email,p.available,p.max_active_workload
        order by active_workload asc,u.full_name asc
        """,nativeQuery=true)
    List<Object[]> aggregateTechnicianReports(@Param("from") Instant from,@Param("to") Instant to,@Param("technicianId") Long technicianId);

    @Query(value="""
        select u.id as technician_id,u.full_name as technician_name,u.email as technician_email,
               coalesce(p.available,false) as available,coalesce(p.max_active_workload,10) as max_active_workload,
               (select count(*) from incidents active_i where active_i.assignee_id=u.id
                 and active_i.status in ('NEW','OPEN','CLASSIFIED','ASSIGNED','ACKNOWLEDGED','IN_PROGRESS')) as active_workload
        from users u left join technician_profiles p on p.user_id=u.id
        where u.active=true and u.role in ('TECHNICIAN','AGENT') order by u.full_name asc
        """,nativeQuery=true)
    List<Object[]> directoryWithWorkload();
}
