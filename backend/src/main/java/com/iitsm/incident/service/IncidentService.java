package com.iitsm.incident.service;

import com.iitsm.audit.service.AuditService;
import com.iitsm.incident.dto.IncidentActivityResponse;
import com.iitsm.incident.category.service.IncidentCategoryService;
import com.iitsm.incident.entity.*;
import com.iitsm.incident.repository.*;
import com.iitsm.intelligence.priority.*;
import com.iitsm.notification.observer.IncidentEvent;
import com.iitsm.notification.service.NotificationPublisher;
import com.iitsm.sla.service.SlaService;
import com.iitsm.user.entity.*;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.servicecatalog.entity.ItService;
import com.iitsm.servicecatalog.service.ItServiceCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service @Transactional
public class IncidentService {
    private final IncidentRepository incidents; private final IncidentActivityRepository activities; private final UserRepository users;
    private final PriorityStrategyFactory priorities; private final String priorityStrategy;
    private final AuditService audit; private final NotificationPublisher notifications; private final SlaService sla;
    private final IncidentCategoryService categories;
    private final ItServiceCatalog serviceCatalog;
    public IncidentService(IncidentRepository incidents,IncidentActivityRepository activities,UserRepository users,PriorityStrategyFactory priorities,
            @Value("${app.priority.strategy:WEIGHTED}") String priorityStrategy,AuditService audit,NotificationPublisher notifications,SlaService sla,IncidentCategoryService categories,ItServiceCatalog serviceCatalog){
        this.incidents=incidents;this.activities=activities;this.users=users;this.priorities=priorities;this.priorityStrategy=priorityStrategy;this.audit=audit;this.notifications=notifications;this.sla=sla;this.categories=categories;this.serviceCatalog=serviceCatalog;
    }
    public Incident createIncident(String title,String description,String category,String actorEmail,ImpactLevel impact,UrgencyLevel urgency,int businessCriticality,int affectedUsers,String affectedService,int serviceCriticality,Long serviceId){
        User reporter=users.findByEmail(actorEmail).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Unknown user"));
        var categoryRecord=categories.requireActive(category);
        ItService selectedService=serviceId == null ? null : serviceCatalog.requireActive(serviceId);
        String serviceLabel=selectedService == null ? requiredServiceLabel(affectedService) : selectedService.getName();
        Incident incident=new Incident(title,description,category,reporter,impact,urgency,businessCriticality,affectedUsers,serviceLabel,serviceCriticality);
        incident.setCategory(categoryRecord.getName());
        incident.setCategoryDefinition(categoryRecord);
        incident.setServiceDefinition(selectedService);
        incident.classify(calculate(incident)); incident=incidents.saveAndFlush(incident);
        audit.recordAction(incident,reporter,"CREATED",null,incident.getStatus().name(),"Incident created and classified as "+incident.getPriority()+" (score "+incident.getPriorityScore()+")");
        notifications.publishEvent(new IncidentEvent(incident,IncidentEvent.EventType.CREATED,"Incident "+incident.getTicketNumber()+" was created."));
        sla.startTracking(incident);
        return incident;
    }
    @Transactional(readOnly=true) public List<Incident> getIncidentsFor(String email,boolean admin,boolean technician){
        if(admin)return incidents.findAll();User u=user(email);
        if(technician)return incidents.findByAssigneeIdOrAssigneeIsNullOrderByCreatedAtDesc(u.getId());
        return incidents.findByReporterId(u.getId());
    }
    @Transactional(readOnly=true) public Incident findVisibleById(Long id,String email,boolean privileged){Incident i=incident(id);if(!privileged&&!i.getReporter().getEmail().equals(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You may only access incidents you reported");return i;}
    @Transactional(readOnly=true) public Incident findVisibleByRole(Long id,String email,boolean admin,boolean technician){
        Incident i=incident(id);if(admin)return i;User actor=user(email);
        boolean visible=technician?(i.getAssignee()==null||i.getAssignee().getId().equals(actor.getId())):i.getReporter().getId().equals(actor.getId());
        if(!visible)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Incident is outside your permitted workload");return i;
    }
    public Incident update(Long id,String email,boolean admin,boolean technician,String title,String description,String category,ImpactLevel impact,UrgencyLevel urgency,int criticality,int affectedUsers,String affectedService,int serviceCriticality,Long serviceId){
        Incident i=findVisibleByRole(id,email,admin,technician);User actor=user(email);
        if(technician&&!admin&&(i.getAssignee()==null||!i.getAssignee().getId().equals(actor.getId())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the assigned technician may edit this incident");
        IncidentPriority old=i.getPriority();String before=i.getTitle()+" | "+old;
        ItService selectedService=serviceId == null ? preserveMatchingService(i, affectedService) : serviceCatalog.requireActive(serviceId);
        String serviceLabel=selectedService == null ? requiredServiceLabel(affectedService) : selectedService.getName();
        var categoryRecord=categories.requireActiveForUpdate(category, i);
        i.updateDetails(title,description,category,impact,urgency,criticality,affectedUsers,serviceLabel,serviceCriticality);i.setServiceDefinition(selectedService);i.setCategoryDefinition(categoryRecord);i.classify(calculate(i));incidents.save(i);
        audit.recordAction(i,actor,"UPDATED",before,i.getTitle()+" | "+i.getPriority(),"Incident details updated; priority recalculated: "+i.getPriorityExplanation());
        if(old!=i.getPriority()){sla.priorityChanged(i);audit.recordAction(i,actor,"PRIORITY_CHANGED",old.name(),i.getPriority().name(),i.getPriorityExplanation());notifications.publishEvent(new IncidentEvent(i,IncidentEvent.EventType.PRIORITY_CHANGED,i.getPriorityExplanation()));}
        return i;
    }
    public Incident transition(Long id,IncidentStatus target,String details,String email){
        Incident i=incident(id);User actor=user(email);Role role=actor.getRole();
        if(role==Role.AGENT)role=Role.TECHNICIAN;
        if(target==IncidentStatus.NEW||target==IncidentStatus.CLASSIFIED||target==IncidentStatus.ASSIGNED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This state is managed by incident creation, classification, or assignment operations");
        boolean owner=i.getReporter().getId().equals(actor.getId());boolean assigned=i.getAssignee()!=null&&i.getAssignee().getId().equals(actor.getId());
        boolean allowed= role==Role.ADMIN || (role==Role.TECHNICIAN&&assigned&&List.of(IncidentStatus.ACKNOWLEDGED,IncidentStatus.IN_PROGRESS,IncidentStatus.RESOLVED).contains(target)) || (role==Role.EMPLOYEE&&owner&&target==IncidentStatus.VERIFIED);
        if(!allowed)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Your role or incident relationship does not permit this transition");
        if(target==IncidentStatus.ACKNOWLEDGED&&!assigned)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the assigned technician can acknowledge this incident");
        IncidentStatus previous=i.getStatus();
        try {
            if(target==IncidentStatus.ACKNOWLEDGED)i.acknowledge(actor);
            else if(target==IncidentStatus.RESOLVED)i.resolveWith(details);
            else i.transitionTo(target);
        } catch(IllegalStateException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage());}
        incidents.save(i);
        audit.recordAction(i,actor,"STATUS_CHANGED",previous.name(),target.name(),details==null?"Incident status changed":details);
        if(target==IncidentStatus.ACKNOWLEDGED)sla.acknowledged(i,actor);
        if(target==IncidentStatus.RESOLVED){sla.resolved(i,actor);notifications.publishEvent(new IncidentEvent(i,IncidentEvent.EventType.RESOLVED,"Resolution: "+details));}
        else if(target==IncidentStatus.CLOSED)notifications.publishEvent(new IncidentEvent(i,IncidentEvent.EventType.CLOSED,"Incident closed."));
        else notifications.publishEvent(new IncidentEvent(i,IncidentEvent.EventType.STATUS_CHANGED,"Status changed from "+previous+" to "+target+"."));
        return i;
    }
    public List<IncidentActivityResponse> activities(Long incidentId,String email,boolean admin,boolean technician){findVisibleByRole(incidentId,email,admin,technician);return activities.findByIncidentIdOrderByCreatedAtAsc(incidentId).stream().map(IncidentActivityResponse::from).toList();}
    public IncidentActivityResponse addActivity(Long incidentId,String email,boolean admin,boolean technician,String type,String message){
        Incident i=findVisibleByRole(incidentId,email,admin,technician);User actor=user(email);Role role=actor.getRole();if(role==Role.AGENT)role=Role.TECHNICIAN;
        boolean assigned=i.getAssignee()!=null&&i.getAssignee().getId().equals(actor.getId());boolean owner=i.getReporter().getId().equals(actor.getId());
        if((type.equals("INVESTIGATION_NOTE")||type.equals("INFORMATION_REQUEST"))&&!(role==Role.ADMIN||(role==Role.TECHNICIAN&&assigned)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the assigned technician or an administrator may add this activity");
        if(type.equals("EMPLOYEE_RESPONSE")&&!(owner||role==Role.ADMIN))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the reporter may respond");
        var activity=activities.save(new IncidentActivity(i,actor,type,message));
        audit.recordAction(i,actor,type,null,null,message);
        if(type.equals("INFORMATION_REQUEST"))notifications.publishEvent(new IncidentEvent(i,IncidentEvent.EventType.INFORMATION_REQUESTED,message));
        return IncidentActivityResponse.from(activity);
    }
    private PriorityDecision calculate(Incident i){return priorities.get(priorityStrategy).calculate(new PriorityInput(i.getImpact(),i.getUrgency(),i.getBusinessCriticality(),i.getAffectedUserCount(),i.getAffectedService(),i.getServiceCriticality()));}
    private String requiredServiceLabel(String value){if(value==null||value.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select an active IT service or provide an affected service label");return value.trim();}
    private ItService preserveMatchingService(Incident incident,String affectedService){
        ItService existing=incident.getServiceDefinition();
        return existing!=null&&affectedService!=null&&existing.getName().equalsIgnoreCase(affectedService.trim())?existing:null;
    }
    private User user(String email){return users.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Unknown user"));}
    private Incident incident(Long id){return incidents.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found"));}
}
