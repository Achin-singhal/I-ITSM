package com.iitsm.sla.entity;
import com.iitsm.common.BaseEntity;
import com.iitsm.incident.entity.Incident;
import jakarta.persistence.*;
@Entity @Table(name="sla_history")
public class SlaHistoryEntry extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="incident_id",nullable=false) private Incident incident;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tracking_id") private SlaTracking tracking;
 @Column(nullable=false,length=48) private String action;
 @Column(nullable=false,length=1000) private String description;
 protected SlaHistoryEntry(){}
 public SlaHistoryEntry(Incident incident,SlaTracking tracking,String action,String description){this.incident=incident;this.tracking=tracking;this.action=action;this.description=description;}
 public Incident getIncident(){return incident;} public SlaTracking getTracking(){return tracking;} public String getAction(){return action;} public String getDescription(){return description;}
}
