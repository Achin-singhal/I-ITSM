package com.iitsm.incident.dto;
import com.iitsm.incident.entity.IncidentActivity;
import java.time.Instant;
public record IncidentActivityResponse(Long id,Long incidentId,Long actorId,String actorName,String type,String message,Instant createdAt){
 public static IncidentActivityResponse from(IncidentActivity a){return new IncidentActivityResponse(a.getId(),a.getIncident().getId(),a.getActor().getId(),a.getActor().getFullName(),a.getActivityType(),a.getMessage(),a.getCreatedAt());}
}
