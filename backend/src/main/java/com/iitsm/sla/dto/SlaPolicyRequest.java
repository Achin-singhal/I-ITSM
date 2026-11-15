package com.iitsm.sla.dto;
import com.iitsm.incident.entity.IncidentPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public class SlaPolicyRequest {
 @NotBlank @Size(max=100) private String name; @NotNull private IncidentPriority priority;
 @Min(1) private int responseTimeMinutes; @Min(1) private int resolutionTimeMinutes; private boolean active=true;
 public String getName(){return name;} public void setName(String v){name=v;} public IncidentPriority getPriority(){return priority;} public void setPriority(IncidentPriority v){priority=v;}
 public int getResponseTimeMinutes(){return responseTimeMinutes;} public void setResponseTimeMinutes(int v){responseTimeMinutes=v;}
 public int getResolutionTimeMinutes(){return resolutionTimeMinutes;} public void setResolutionTimeMinutes(int v){resolutionTimeMinutes=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
