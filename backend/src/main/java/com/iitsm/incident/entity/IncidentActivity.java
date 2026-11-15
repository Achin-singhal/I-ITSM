package com.iitsm.incident.entity;
import com.iitsm.common.BaseEntity;
import com.iitsm.user.entity.User;
import jakarta.persistence.*;

@Entity @Table(name="incident_activities")
public class IncidentActivity extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="incident_id",nullable=false) private Incident incident;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_id",nullable=false) private User actor;
 @Column(name="activity_type",nullable=false,length=40) private String activityType;
 @Column(nullable=false,columnDefinition="TEXT") private String message;
 protected IncidentActivity(){}
 public IncidentActivity(Incident incident,User actor,String type,String message){this.incident=incident;this.actor=actor;this.activityType=type;this.message=message;}
 public Incident getIncident(){return incident;} public User getActor(){return actor;} public String getActivityType(){return activityType;} public String getMessage(){return message;}
}
