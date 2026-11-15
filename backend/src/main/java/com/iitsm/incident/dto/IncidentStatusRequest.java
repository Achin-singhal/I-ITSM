package com.iitsm.incident.dto;

import com.iitsm.incident.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public class IncidentStatusRequest {
    @NotNull private IncidentStatus status;
    @jakarta.validation.constraints.Size(max=2000) private String details;
    public IncidentStatusRequest() {}
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public String getDetails(){return details;} public void setDetails(String details){this.details=details;}
}
