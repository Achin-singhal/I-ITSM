package com.iitsm.sla.dto;
import com.iitsm.sla.entity.SlaPolicy;
public record SlaPolicyResponse(Long id,String name,String priority,int responseTimeMinutes,int resolutionTimeMinutes,boolean active){
 public static SlaPolicyResponse from(SlaPolicy p){return new SlaPolicyResponse(p.getId(),p.getName(),p.getPriority().name(),p.getResponseTimeMinutes(),p.getResolutionTimeMinutes(),p.isActive());}
}
