package com.iitsm.assignment.dto;
import com.iitsm.assignment.entity.TechnicianProfile;
public record TechnicianRecommendation(Long technicianId,String name,String email,long activeWorkload,int maxActiveWorkload,int experienceYears,String strategy) {
 public static TechnicianRecommendation of(TechnicianProfile p,long workload,String strategy){return new TechnicianRecommendation(p.getUser().getId(),p.getUser().getFullName(),p.getUser().getEmail(),workload,p.getMaxActiveWorkload(),p.getExperienceYears(),strategy);}
}
