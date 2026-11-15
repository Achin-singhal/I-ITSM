package com.iitsm.sla.dto;
import com.iitsm.sla.entity.SlaTracking;
import java.time.Instant;
public record SlaStatusResponse(Long incidentId,String policy,int responseTargetMinutes,int resolutionTargetMinutes,
 Instant responseDeadline,Instant resolutionDeadline,Instant acknowledgedAt,Instant resolvedAt,String status,
 boolean responseViolated,boolean resolutionViolated){
 public static SlaStatusResponse from(SlaTracking t){return new SlaStatusResponse(t.getIncident().getId(),t.getPolicy().getName(),t.getPolicy().getResponseTimeMinutes(),t.getPolicy().getResolutionTimeMinutes(),t.getResponseDeadline(),t.getResolutionDeadline(),t.getAcknowledgedAt(),t.getResolvedAt(),t.getStatus().name(),t.isResponseViolationRecorded(),t.isResolutionViolationRecorded());}
}
