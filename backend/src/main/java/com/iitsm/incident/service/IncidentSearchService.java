package com.iitsm.incident.service;

import com.iitsm.common.PagedResponse;
import com.iitsm.incident.dto.IncidentResponse;
import com.iitsm.incident.dto.IncidentSearchCriteria;
import com.iitsm.incident.repository.IncidentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IncidentSearchService {
    private final IncidentRepository incidents;
    public IncidentSearchService(IncidentRepository incidents) { this.incidents = incidents; }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> search(IncidentSearchCriteria criteria, Long reporterId, Long technicianScopeId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be nonnegative and size must be between 1 and 100");
        if (criteria.createdFrom() != null && criteria.createdTo() != null && !criteria.createdFrom().isBefore(criteria.createdTo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdFrom must be earlier than createdTo");
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PagedResponse.from(incidents.searchIncidents(criteria.incidentId(), blankToNull(criteria.query()), criteria.status(),
                criteria.priority(), blankToNull(criteria.category()), criteria.assigneeId(), reporterId, technicianScopeId, criteria.createdFrom(),
                criteria.createdTo(), criteria.slaStatus(), pageable).map(com.iitsm.incident.dto.IncidentResponse::from));
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
