package com.iitsm.incident.dto;

import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.sla.entity.SlaStatus;
import java.time.Instant;

public record IncidentSearchCriteria(Long incidentId, String query, IncidentStatus status,
        IncidentPriority priority, String category, Long assigneeId, Instant createdFrom,
        Instant createdTo, SlaStatus slaStatus) { }
