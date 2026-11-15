package com.iitsm.sla.entity;

import com.iitsm.common.BaseEntity;
import com.iitsm.incident.entity.Incident;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="sla_tracking",uniqueConstraints=@UniqueConstraint(name="uk_sla_tracking_incident",columnNames="incident_id"))
public class SlaTracking extends BaseEntity {
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="incident_id",nullable=false) private Incident incident;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="policy_id",nullable=false) private SlaPolicy policy;
    @Column(name="response_deadline",nullable=false) private Instant responseDeadline;
    @Column(name="resolution_deadline",nullable=false) private Instant resolutionDeadline;
    @Column(name="acknowledged_at") private Instant acknowledgedAt;
    @Column(name="resolved_at") private Instant resolvedAt;
    @Column(name="response_warning_sent",nullable=false) private boolean responseWarningSent;
    @Column(name="resolution_warning_sent",nullable=false) private boolean resolutionWarningSent;
    @Column(name="response_escalation_sent",nullable=false) private boolean responseEscalationSent;
    @Column(name="resolution_escalation_sent",nullable=false) private boolean resolutionEscalationSent;
    @Column(name="response_violation_recorded",nullable=false) private boolean responseViolationRecorded;
    @Column(name="resolution_violation_recorded",nullable=false) private boolean resolutionViolationRecorded;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private SlaStatus status=SlaStatus.HEALTHY;
    protected SlaTracking(){}
    public SlaTracking(Incident incident,SlaPolicy policy){this.incident=incident;this.policy=policy;this.responseDeadline=incident.getCreatedAt().plusSeconds(policy.getResponseTimeMinutes()*60L);this.resolutionDeadline=incident.getCreatedAt().plusSeconds(policy.getResolutionTimeMinutes()*60L);}
    public Incident getIncident(){return incident;} public SlaPolicy getPolicy(){return policy;} public Instant getResponseDeadline(){return responseDeadline;} public Instant getResolutionDeadline(){return resolutionDeadline;}
    public Instant getAcknowledgedAt(){return acknowledgedAt;} public Instant getResolvedAt(){return resolvedAt;} public SlaStatus getStatus(){return status;}
    public boolean isResponseWarningSent(){return responseWarningSent;} public boolean isResolutionWarningSent(){return resolutionWarningSent;}
    public boolean isResponseEscalationSent(){return responseEscalationSent;} public boolean isResolutionEscalationSent(){return resolutionEscalationSent;}
    public boolean isResponseViolationRecorded(){return responseViolationRecorded;} public boolean isResolutionViolationRecorded(){return resolutionViolationRecorded;}
    public boolean setAcknowledgedAt(Instant value){if(acknowledgedAt!=null)return false;acknowledgedAt=value;return true;}
    public boolean setResolvedAt(Instant value){if(resolvedAt!=null)return false;resolvedAt=value;return true;}
    public boolean setResponseWarningSent(){if(responseWarningSent)return false;responseWarningSent=true;return true;}
    public boolean setResolutionWarningSent(){if(resolutionWarningSent)return false;resolutionWarningSent=true;return true;}
    public boolean setResponseEscalationSent(){if(responseEscalationSent)return false;responseEscalationSent=true;return true;}
    public boolean setResolutionEscalationSent(){if(resolutionEscalationSent)return false;resolutionEscalationSent=true;return true;}
    public boolean recordResponseViolation(){if(responseViolationRecorded)return false;responseViolationRecorded=true;status=SlaStatus.BREACHED;return true;}
    public boolean recordResolutionViolation(){if(resolutionViolationRecorded)return false;resolutionViolationRecorded=true;status=SlaStatus.BREACHED;return true;}
    public void markAtRisk(){if(status==SlaStatus.HEALTHY)status=SlaStatus.AT_RISK;}
    public void updatePolicy(SlaPolicy policy){this.policy=policy;this.responseDeadline=incident.getCreatedAt().plusSeconds(policy.getResponseTimeMinutes()*60L);this.resolutionDeadline=incident.getCreatedAt().plusSeconds(policy.getResolutionTimeMinutes()*60L);this.responseWarningSent=false;this.resolutionWarningSent=false;this.responseEscalationSent=false;this.resolutionEscalationSent=false;}
}
