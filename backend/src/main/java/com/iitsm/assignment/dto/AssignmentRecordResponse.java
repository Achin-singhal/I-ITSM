package com.iitsm.assignment.dto;
import com.iitsm.assignment.entity.AssignmentRecord;
import java.time.Instant;
public record AssignmentRecordResponse(Long id,Long incidentId,Long technicianId,String technicianName,Long assignedById,String strategy,String reason,Instant assignedAt) {
 public static AssignmentRecordResponse from(AssignmentRecord a){return new AssignmentRecordResponse(a.getId(),a.getIncident().getId(),a.getAssignedTo().getId(),a.getAssignedTo().getFullName(),a.getAssignedBy()==null?null:a.getAssignedBy().getId(),a.getStrategyUsed(),a.getReason(),a.getCreatedAt());}
}
