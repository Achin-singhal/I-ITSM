package com.iitsm.audit.service;

import com.iitsm.audit.dto.AuditResponse;
import com.iitsm.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

@Service @Transactional(readOnly=true)
public class AuditQueryService {
    private final AuditLogRepository repository;
    public AuditQueryService(AuditLogRepository repository) { this.repository=repository; }
    public List<AuditResponse> search(Long incidentId, Long actorId, String action, String targetEntity,
                                      Long targetId, Instant from, Instant to) {
        if(from!=null&&to!=null&&from.isAfter(to))throw new IllegalArgumentException("Audit filter start must be before end");
        var rows=repository.findAll((root,query,cb)->{
            List<Predicate> predicates=new ArrayList<>();
            if(incidentId!=null)predicates.add(cb.equal(root.join("incident").get("id"),incidentId));
            if(actorId!=null)predicates.add(cb.equal(root.join("actor").get("id"),actorId));
            if(action!=null&&!action.isBlank())predicates.add(cb.equal(root.get("action"),action));
            if(targetEntity!=null&&!targetEntity.isBlank())predicates.add(cb.equal(root.get("targetEntity"),targetEntity.trim().toUpperCase(java.util.Locale.ROOT)));
            if(targetId!=null)predicates.add(cb.equal(root.get("targetId"),targetId));
            if(from!=null)predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from));
            if(to!=null)predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),to));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.DESC,"createdAt"));
        return rows.stream().map(AuditResponse::from).toList();
    }
}
