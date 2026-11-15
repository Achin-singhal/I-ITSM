package com.iitsm.sla.entity;

import com.iitsm.common.BaseEntity;
import com.iitsm.incident.entity.IncidentPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Service Level Agreement policy defining target response and resolution durations.
 */
@Entity
@Table(name = "sla_policies")
public class SlaPolicy extends BaseEntity {

    @NotBlank(message = "Policy name is required")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private IncidentPriority priority;

    @Column(name = "response_time_minutes", nullable = false)
    private int responseTimeMinutes;

    @Column(name = "resolution_time_minutes", nullable = false)
    private int resolutionTimeMinutes;

    @Column(nullable = false)
    private boolean active = true;

    public SlaPolicy() {
    }

    public SlaPolicy(String name, IncidentPriority priority, int responseTimeMinutes, int resolutionTimeMinutes) {
        validateTargets(responseTimeMinutes,resolutionTimeMinutes);
        this.name = name;
        this.priority = priority;
        this.responseTimeMinutes = responseTimeMinutes;
        this.resolutionTimeMinutes = resolutionTimeMinutes;
        this.active = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public int getResponseTimeMinutes() {
        return responseTimeMinutes;
    }

    public void setResponseTimeMinutes(int responseTimeMinutes) {
        validateTargets(responseTimeMinutes,resolutionTimeMinutes);
        this.responseTimeMinutes = responseTimeMinutes;
    }

    public int getResolutionTimeMinutes() {
        return resolutionTimeMinutes;
    }

    public void setResolutionTimeMinutes(int resolutionTimeMinutes) {
        validateTargets(responseTimeMinutes,resolutionTimeMinutes);
        this.resolutionTimeMinutes = resolutionTimeMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void configure(String name,IncidentPriority priority,int responseMinutes,int resolutionMinutes,boolean active){
        validateTargets(responseMinutes,resolutionMinutes);
        if(name==null||name.isBlank()||priority==null)throw new IllegalArgumentException("SLA policy name and priority are required");
        this.name=name;this.priority=priority;this.responseTimeMinutes=responseMinutes;this.resolutionTimeMinutes=resolutionMinutes;this.active=active;
    }

    private void validateTargets(int response,int resolution){if(response<1||resolution<1)throw new IllegalArgumentException("SLA targets must be positive minutes");}
}
