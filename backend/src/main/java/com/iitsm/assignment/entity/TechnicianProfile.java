package com.iitsm.assignment.entity;

import com.iitsm.common.BaseEntity;
import com.iitsm.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name="technician_profiles")
public class TechnicianProfile extends BaseEntity {
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",nullable=false,unique=true)
    private User user;
    @Column(nullable=false) private boolean available=true;
    @Min(0) @Column(name="experience_years",nullable=false) private int experienceYears;
    @Min(1) @Column(name="max_active_workload",nullable=false) private int maxActiveWorkload=10;
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="technician_skills",joinColumns=@JoinColumn(name="profile_id"))
    @Column(name="skill",nullable=false,length=64) private Set<String> skills=new HashSet<>();
    public TechnicianProfile() {}
    public TechnicianProfile(User user, boolean available, int experienceYears, int maxActiveWorkload, Set<String> skills) {
        this.user=user; this.available=available; this.experienceYears=experienceYears; this.maxActiveWorkload=maxActiveWorkload; setSkills(skills);
    }
    public User getUser(){return user;} public boolean isAvailable(){return available;} public int getExperienceYears(){return experienceYears;}
    public int getMaxActiveWorkload(){return maxActiveWorkload;} public Set<String> getSkills(){return Set.copyOf(skills);}
    public void configure(boolean available,int experienceYears,int maxActiveWorkload,Set<String> skills) {
        if(experienceYears<0||maxActiveWorkload<1) throw new IllegalArgumentException("Experience must be nonnegative and max workload must be positive");
        this.available=available; this.experienceYears=experienceYears; this.maxActiveWorkload=maxActiveWorkload; setSkills(skills);
    }
    private void setSkills(Set<String> values){this.skills=new HashSet<>();if(values!=null)values.stream().filter(s->s!=null&&!s.isBlank()).map(String::trim).map(String::toUpperCase).forEach(this.skills::add);}
}
