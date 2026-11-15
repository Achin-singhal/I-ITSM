package com.iitsm.sla.dto;
import com.iitsm.sla.entity.SlaHistoryEntry;
import java.time.Instant;
public record SlaHistoryResponse(Long id,Long incidentId,String action,String description,Instant timestamp){
 public static SlaHistoryResponse from(SlaHistoryEntry e){return new SlaHistoryResponse(e.getId(),e.getIncident().getId(),e.getAction(),e.getDescription(),e.getCreatedAt());}
}
