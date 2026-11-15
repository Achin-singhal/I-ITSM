package com.iitsm.sla.service;

import com.iitsm.audit.service.AuditService;
import com.iitsm.incident.entity.*;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.notification.observer.IncidentEvent;
import com.iitsm.notification.service.NotificationPublisher;
import com.iitsm.sla.dto.*;
import com.iitsm.sla.entity.*;
import com.iitsm.sla.repository.*;
import com.iitsm.sla.entity.SlaHistoryEntry;
import com.iitsm.sla.dto.SlaHistoryResponse;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SlaService {
    private final SlaPolicyRepository policies; private final SlaTrackingRepository tracking; private final IncidentRepository incidents;
    private final UserRepository users; private final AuditService audit; private final NotificationPublisher notifications; private final SlaHistoryRepository history;
    private final double warningThresholdPercent; private final double escalationThresholdPercent;
    public SlaService(SlaPolicyRepository policies,SlaTrackingRepository tracking,IncidentRepository incidents,UserRepository users,AuditService audit,NotificationPublisher notifications,SlaHistoryRepository history){
        this(policies,tracking,incidents,users,audit,notifications,history,0.20,0.10);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public SlaService(SlaPolicyRepository policies,SlaTrackingRepository tracking,IncidentRepository incidents,UserRepository users,AuditService audit,NotificationPublisher notifications,SlaHistoryRepository history,
            @org.springframework.beans.factory.annotation.Value("${app.sla.warning-threshold-percent:0.20}") double warningThresholdPercent,
            @org.springframework.beans.factory.annotation.Value("${app.sla.escalation-threshold-percent:0.10}") double escalationThresholdPercent){
        if(!Double.isFinite(warningThresholdPercent)||warningThresholdPercent<=0||warningThresholdPercent>1)throw new IllegalArgumentException("app.sla.warning-threshold-percent must be greater than 0 and at most 1");
        if(!Double.isFinite(escalationThresholdPercent)||escalationThresholdPercent<=0||escalationThresholdPercent>=warningThresholdPercent)throw new IllegalArgumentException("app.sla.escalation-threshold-percent must be greater than 0 and less than warning-threshold-percent");
        this.policies=policies;this.tracking=tracking;this.incidents=incidents;this.users=users;this.audit=audit;this.notifications=notifications;this.history=history;
        this.warningThresholdPercent=warningThresholdPercent;this.escalationThresholdPercent=escalationThresholdPercent;
    }
    @Transactional public SlaTracking startTracking(Incident incident) {
        SlaPolicy policy=policies.findByPriorityAndActiveTrue(incident.getPriority()).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"No active SLA policy configured for "+incident.getPriority()));
        SlaTracking saved=tracking.save(new SlaTracking(incident,policy));history.save(new SlaHistoryEntry(incident,saved,"SLA_STARTED","Applied "+policy.getName()+". Response by "+saved.getResponseDeadline()+", resolution by "+saved.getResolutionDeadline()+"."));return saved;
    }
    @Transactional public void priorityChanged(Incident incident) {
        tracking.findByIncidentIdForUpdate(incident.getId()).ifPresent(t->{SlaPolicy p=policies.findByPriorityAndActiveTrue(incident.getPriority()).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"No active SLA policy configured for "+incident.getPriority()));t.updatePolicy(p);history.save(new SlaHistoryEntry(incident,t,"SLA_POLICY_CHANGED","New policy "+p.getName()+" set response deadline "+t.getResponseDeadline()+" and resolution deadline "+t.getResolutionDeadline()+"."));});
    }
    @Transactional public void acknowledged(Incident incident,User actor) { tracking.findByIncidentIdForUpdate(incident.getId()).ifPresent(t->{Instant now=Instant.now();if(t.setAcknowledgedAt(now)){history.save(new SlaHistoryEntry(incident,t,"ACKNOWLEDGED","Response acknowledged by "+actor.getEmail()+"."));if(now.isAfter(t.getResponseDeadline())&&t.recordResponseViolation()){emit(t,"SLA_RESPONSE_VIOLATED","ON_TIME","BREACHED","Response SLA deadline was missed",IncidentEvent.EventType.SLA_VIOLATED);escalate(t,"SLA_ESCALATED_RESPONSE_BREACH","Response SLA deadline was breached");}}}); }
    @Transactional public void resolved(Incident incident,User actor) { tracking.findByIncidentIdForUpdate(incident.getId()).ifPresent(t->{Instant now=Instant.now();if(t.setResolvedAt(now)){history.save(new SlaHistoryEntry(incident,t,"RESOLVED","Incident resolved by "+actor.getEmail()+"."));if(now.isAfter(t.getResolutionDeadline())&&t.recordResolutionViolation()){emit(t,"SLA_RESOLUTION_VIOLATED","ON_TIME","BREACHED","Resolution SLA deadline was missed",IncidentEvent.EventType.SLA_VIOLATED);escalate(t,"SLA_ESCALATED_RESOLUTION_BREACH","Resolution SLA deadline was breached");}}}); }
    @Transactional(readOnly=true) public List<SlaPolicyResponse> listPolicies(){return policies.findAll().stream().map(SlaPolicyResponse::from).toList();}
    @Transactional public SlaPolicyResponse savePolicy(SlaPolicyRequest request,Long id){
        SlaPolicy p=id==null?new SlaPolicy(request.getName(),request.getPriority(),request.getResponseTimeMinutes(),request.getResolutionTimeMinutes()):policies.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"SLA policy not found"));
        if(id!=null)p.configure(request.getName(),request.getPriority(),request.getResponseTimeMinutes(),request.getResolutionTimeMinutes(),request.isActive());
        else p.setActive(request.isActive());
        p=policies.save(p);
        final SlaPolicy saved=p;
        tracking.lockAllForUpdate().stream().filter(t->t.getPolicy().getPriority()==saved.getPriority() && saved.isActive()).forEach(t->{t.updatePolicy(saved);history.save(new SlaHistoryEntry(t.getIncident(),t,"SLA_POLICY_CHANGED","Policy "+saved.getName()+" changed; current deadlines are "+t.getResponseDeadline()+" and "+t.getResolutionDeadline()+"."));});
        return SlaPolicyResponse.from(p);
    }
    @Transactional public void deactivatePolicy(Long id){SlaPolicy p=policies.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"SLA policy not found"));p.setActive(false);}
    @Transactional(readOnly=true) public SlaStatusResponse status(Long incidentId){return SlaStatusResponse.from(tracking.findByIncidentId(incidentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"SLA tracking not found")));}
    @Transactional(readOnly=true) public List<SlaHistoryResponse> history(Long incidentId){return history.findByIncidentIdOrderByCreatedAtAsc(incidentId).stream().map(SlaHistoryResponse::from).toList();}

    @Scheduled(fixedDelayString="${app.sla.monitor-interval-ms:60000}")
    @Transactional
    public void monitorDeadlines(){
        Instant now=Instant.now();
        for(SlaTracking item:tracking.lockUnresolved()){
            Incident incident=item.getIncident();
            if(item.getAcknowledgedAt()==null){
                Duration target=Duration.ofMinutes(item.getPolicy().getResponseTimeMinutes());
                if(!item.isResponseWarningSent() && Duration.between(now,item.getResponseDeadline()).compareTo(thresholdWindow(target,warningThresholdPercent))<=0 && now.isBefore(item.getResponseDeadline())){
                    item.setResponseWarningSent(); item.markAtRisk(); emit(item,"SLA_RESPONSE_WARNING","HEALTHY","AT_RISK","Response target is approaching",IncidentEvent.EventType.SLA_WARNING);
                }
                if(!item.isResponseEscalationSent() && Duration.between(now,item.getResponseDeadline()).compareTo(thresholdWindow(target,escalationThresholdPercent))<=0 && now.isBefore(item.getResponseDeadline())){
                    item.setResponseEscalationSent(); escalate(item,"SLA_ESCALATED_RESPONSE_WARNING","Response SLA deadline is within the escalation threshold");
                }
                if(!item.isResponseViolationRecorded() && !now.isBefore(item.getResponseDeadline())){
                    item.recordResponseViolation(); emit(item,"SLA_RESPONSE_VIOLATED","ON_TIME","BREACHED","Response SLA deadline was missed",IncidentEvent.EventType.SLA_VIOLATED); escalate(item,"SLA_ESCALATED_RESPONSE_BREACH","Response SLA deadline was breached");
                }
            }
            Duration resolutionTarget=Duration.ofMinutes(item.getPolicy().getResolutionTimeMinutes());
            if(!item.isResolutionWarningSent() && Duration.between(now,item.getResolutionDeadline()).compareTo(thresholdWindow(resolutionTarget,warningThresholdPercent))<=0 && now.isBefore(item.getResolutionDeadline())){
                item.setResolutionWarningSent(); item.markAtRisk(); emit(item,"SLA_RESOLUTION_WARNING","HEALTHY","AT_RISK","Resolution target is approaching",IncidentEvent.EventType.SLA_WARNING);
            }
            if(!item.isResolutionEscalationSent() && Duration.between(now,item.getResolutionDeadline()).compareTo(thresholdWindow(resolutionTarget,escalationThresholdPercent))<=0 && now.isBefore(item.getResolutionDeadline())){
                item.setResolutionEscalationSent(); escalate(item,"SLA_ESCALATED_RESOLUTION_WARNING","Resolution SLA deadline is within the escalation threshold");
            }
            if(!item.isResolutionViolationRecorded() && !now.isBefore(item.getResolutionDeadline())){
                item.recordResolutionViolation(); emit(item,"SLA_RESOLUTION_VIOLATED","ON_TIME","BREACHED","Resolution SLA deadline was missed",IncidentEvent.EventType.SLA_VIOLATED); escalate(item,"SLA_ESCALATED_RESOLUTION_BREACH","Resolution SLA deadline was breached");
            }
        }
    }
    private Duration thresholdWindow(Duration target,double percent){return Duration.ofMillis(Math.max(1,(long)(target.toMillis()*percent)));}
    private void escalate(SlaTracking item,String action,String description){
        history.save(new SlaHistoryEntry(item.getIncident(),item,action,description));
        audit.recordAction(item.getIncident(),null,action,"MONITORED","ESCALATED",description);
        notifications.publishEvent(new IncidentEvent(item.getIncident(),IncidentEvent.EventType.SLA_ESCALATED,description));
    }
    private void emit(SlaTracking item,String action,String oldValue,String newValue,String description,IncidentEvent.EventType event){
        Incident incident=item.getIncident();
        history.save(new SlaHistoryEntry(incident,item,action,description));
        audit.recordAction(incident,null,action,oldValue,newValue,description);
        notifications.publishEvent(new IncidentEvent(incident,event,description));
    }
}
