package com.iitsm.incident.dto;

import jakarta.validation.constraints.NotNull;

public class IncidentAssignmentRequest {
    @NotNull private Long technicianId;
    public IncidentAssignmentRequest() {}
    public Long getTechnicianId() { return technicianId; }
    public void setTechnicianId(Long technicianId) { this.technicianId = technicianId; }
}
