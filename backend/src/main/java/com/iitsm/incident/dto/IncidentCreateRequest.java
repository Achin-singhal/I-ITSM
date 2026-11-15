package com.iitsm.incident.dto;

import com.iitsm.incident.entity.ImpactLevel;
import com.iitsm.incident.entity.UrgencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IncidentCreateRequest {
    @NotBlank @Size(max = 200) private String title;
    @NotBlank @Size(max=10000) private String description;
    @NotNull private ImpactLevel impact;
    @NotNull private UrgencyLevel urgency;
    @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) private int businessCriticality;
    @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(1000000) private int affectedUserCount;
    @Size(max=120) private String affectedService;
    @jakarta.validation.constraints.Positive private Long serviceId;
    @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) private int serviceCriticality;
    @NotBlank @Size(max = 64) private String category;
    public IncidentCreateRequest() {}
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ImpactLevel getImpact() { return impact; }
    public void setImpact(ImpactLevel impact) { this.impact = impact; }
    public UrgencyLevel getUrgency() { return urgency; }
    public void setUrgency(UrgencyLevel urgency) { this.urgency = urgency; }
    public int getBusinessCriticality() { return businessCriticality; }
    public void setBusinessCriticality(int businessCriticality) { this.businessCriticality = businessCriticality; }
    public int getAffectedUserCount() { return affectedUserCount; }
    public void setAffectedUserCount(int affectedUserCount) { this.affectedUserCount = affectedUserCount; }
    public String getAffectedService() { return affectedService; }
    public void setAffectedService(String affectedService) { this.affectedService = affectedService; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public int getServiceCriticality() { return serviceCriticality; }
    public void setServiceCriticality(int serviceCriticality) { this.serviceCriticality = serviceCriticality; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
