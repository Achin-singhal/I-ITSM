package com.iitsm.assignment.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
public class TechnicianProfileRequest {
    private boolean available=true;
    @Min(0) private int experienceYears;
    @Min(1) private int maxActiveWorkload=10;
    @NotNull @Size(min=1) private Set<@Size(min=1,max=64) String> skills;
    public boolean isAvailable(){return available;} public void setAvailable(boolean v){available=v;}
    public int getExperienceYears(){return experienceYears;} public void setExperienceYears(int v){experienceYears=v;}
    public int getMaxActiveWorkload(){return maxActiveWorkload;} public void setMaxActiveWorkload(int v){maxActiveWorkload=v;}
    public Set<String> getSkills(){return skills;} public void setSkills(Set<String> v){skills=v;}
}
