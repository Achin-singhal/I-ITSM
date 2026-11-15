package com.iitsm.assignment.service;

import com.iitsm.assignment.dto.TechnicianRecommendation;
import com.iitsm.assignment.dto.RankedTechnicianRecommendation;
import com.iitsm.report.dto.TechnicianDirectoryResponse;
import com.iitsm.assignment.entity.*;
import com.iitsm.assignment.repository.*;
import com.iitsm.assignment.strategy.*;
import com.iitsm.audit.service.AuditService;
import com.iitsm.incident.entity.*;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.notification.observer.IncidentEvent;
import com.iitsm.notification.service.NotificationPublisher;
import com.iitsm.user.entity.*;
import com.iitsm.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class AssignmentService {
    private static final List<IncidentStatus> ACTIVE=List.of(IncidentStatus.NEW,IncidentStatus.CLASSIFIED,IncidentStatus.ASSIGNED,IncidentStatus.ACKNOWLEDGED,IncidentStatus.IN_PROGRESS,IncidentStatus.OPEN);
    private final IncidentRepository incidents; private final UserRepository users; private final TechnicianProfileRepository profiles;
    private final AssignmentRecordRepository records; private final AssignmentConfigurationRepository configs;
    private final AssignmentStrategyFactory strategies; private final AuditService audit; private final NotificationPublisher notifications;
    public AssignmentService(IncidentRepository incidents,UserRepository users,TechnicianProfileRepository profiles,AssignmentRecordRepository records,
            AssignmentConfigurationRepository configs,AssignmentStrategyFactory strategies,AuditService audit,NotificationPublisher notifications) {
        this.incidents=incidents;this.users=users;this.profiles=profiles;this.records=records;this.configs=configs;this.strategies=strategies;this.audit=audit;this.notifications=notifications;
    }
    @Transactional(readOnly=true) public TechnicianRecommendation recommend(Long incidentId,String strategyName) {
        Incident incident=incidents.findById(incidentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found"));
        String name=strategyName==null?configs.findById(1L).map(AssignmentConfiguration::getStrategyName).orElse("WORKLOAD_BALANCED"):strategyName;
        var strategy=strategies.getStrategy(name).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown assignment strategy"));
        TechnicianProfile p=strategy.recommend(incident,candidates()).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"No eligible technician is available"));
        return TechnicianRecommendation.of(p,workload(p),name);
    }
    @Transactional(readOnly=true) public List<RankedTechnicianRecommendation> rankedRecommendations(Long incidentId,String strategyName) {
        Incident incident=incidents.findById(incidentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found"));
        String name=strategyName==null?configs.findById(1L).map(AssignmentConfiguration::getStrategyName).orElse("WORKLOAD_BALANCED"):strategyName;
        AssignmentStrategy strategy=strategies.getStrategy(name).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown assignment strategy"));
        List<TechnicianCandidate> ranked=strategy.rank(incident,candidates());
        String category=incident.getCategory().trim().toUpperCase();
        return java.util.stream.IntStream.range(0,ranked.size()).mapToObj(index->{
            TechnicianCandidate candidate=ranked.get(index); TechnicianProfile p=candidate.profile();
            boolean skill=p.getSkills().contains(category)||p.getSkills().contains("*");
            List<String> reasons=new ArrayList<>();
            reasons.add(skill?"Skill match: "+category:"No exact skill match; ranked by capacity and experience");
            reasons.add("Active workload "+candidate.activeWorkload()+" of "+p.getMaxActiveWorkload());
            if(p.getExperienceYears()>0) reasons.add(p.getExperienceYears()+" years relevant experience on profile");
            return new RankedTechnicianRecommendation(p.getUser().getId(),p.getUser().getFullName(),p.getUser().getEmail(),candidate.activeWorkload(),p.getMaxActiveWorkload(),p.getExperienceYears(),name,index+1,List.copyOf(reasons));
        }).toList();
    }
    @Transactional public TechnicianRecommendation assign(Long incidentId,String actorEmail,String strategyName) {
        return assignInternal(incidentId,actorEmail,strategyName,null);
    }
    @Transactional public TechnicianRecommendation assignSelected(Long incidentId,String actorEmail,String strategyName,Long technicianId) {
        return assignInternal(incidentId,actorEmail,strategyName,technicianId);
    }
    private TechnicianRecommendation assignInternal(Long incidentId,String actorEmail,String strategyName,Long requestedTechnicianId) {
        // Serialize concurrent assignment attempts for the same incident before
        // validating its lifecycle state or changing its assignee.
        Incident incident=incidents.findByIdForUpdate(incidentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found"));
        User actor=users.findByEmail(actorEmail).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Role actorRole=actor.getRole()==Role.AGENT?Role.TECHNICIAN:actor.getRole();
        if(actorRole!=Role.TECHNICIAN&&actorRole!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only technicians or administrators can assign incidents");
        if(incident.getStatus()==IncidentStatus.ASSIGNED&&incident.getAssignee()!=null&&actorRole!=Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only an administrator can reassign an assigned incident");
        boolean eligibleForInitialAssignment=incident.getAssignee()==null&&List.of(IncidentStatus.NEW,IncidentStatus.CLASSIFIED,IncidentStatus.OPEN).contains(incident.getStatus());
        boolean administratorReassignment=actorRole==Role.ADMIN&&incident.getAssignee()!=null&&List.of(IncidentStatus.OPEN,IncidentStatus.ASSIGNED).contains(incident.getStatus());
        if(!eligibleForInitialAssignment&&!administratorReassignment)throw new ResponseStatusException(HttpStatus.CONFLICT,"Incident cannot be assigned or reassigned in its current state");
        String name=strategyName==null?configs.findById(1L).map(AssignmentConfiguration::getStrategyName).orElse("WORKLOAD_BALANCED"):strategyName;
        var strategy=strategies.getStrategy(name).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown assignment strategy"));
        // Lock candidate rows in a stable order before counting active work. Concurrent
        // assignments then observe the previous transaction's committed workload.
        List<TechnicianCandidate> candidates=lockedCandidates();
        List<TechnicianCandidate> ranked=strategy.rank(incident,candidates);
        TechnicianProfile selected=ranked.stream().map(TechnicianCandidate::profile)
                .filter(p->requestedTechnicianId==null||p.getUser().getId().equals(requestedTechnicianId)).findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,requestedTechnicianId==null?"No eligible technician is available":"Selected technician is not an eligible recommendation"));
        if(administratorReassignment&&selected.getUser().getId().equals(incident.getAssignee().getId()))
            return TechnicianRecommendation.of(selected,workload(selected),name);
        IncidentStatus previous=incident.getStatus();
        User previousAssignee=incident.getAssignee();
        incident.assignTo(selected.getUser()); incidents.save(incident);
        records.save(new AssignmentRecord(incident,selected.getUser(),actor,name,"Recommended by "+name+" strategy"));
        String action=previousAssignee==null?"ASSIGNED":"REASSIGNED";
        String description=previousAssignee==null
                ?"Assigned to technician "+selected.getUser().getEmail()+" using "+name
                :"Reassigned from "+previousAssignee.getEmail()+" to "+selected.getUser().getEmail()+" using "+name;
        audit.recordAction(incident,actor,action,previousAssignee==null?previous.name():previousAssignee.getEmail(),
                previousAssignee==null?incident.getStatus().name():selected.getUser().getEmail(),description);
        notifications.publishEvent(new IncidentEvent(incident,IncidentEvent.EventType.ASSIGNED,"Assigned to "+selected.getUser().getFullName()+"."));
        return TechnicianRecommendation.of(selected,workload(selected),name);
    }
    @Transactional public String configureStrategy(String name) {
        strategies.getStrategy(name).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported assignment strategy"));
        var config=configs.findById(1L).orElseGet(()->new AssignmentConfiguration(name)); config.setStrategyName(name.toUpperCase()); configs.save(config); return config.getStrategyName();
    }
    @Transactional public TechnicianProfile configureProfile(Long userId,boolean available,int experience,int maxWorkload,Set<String> skills) {
        User user=users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        if(user.getRole()!=Role.TECHNICIAN && user.getRole()!=Role.AGENT) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"User is not a technician");
        TechnicianProfile p=profiles.findByUserIdForUpdate(userId).orElseGet(()->new TechnicianProfile(user,available,experience,maxWorkload,skills));
        if(p.getId()!=null)p.configure(available,experience,maxWorkload,skills);
        return profiles.save(p);
    }
    @Transactional(readOnly=true) public List<com.iitsm.assignment.entity.AssignmentRecord> history(Long incidentId) { return records.findByIncidentIdOrderByCreatedAtDesc(incidentId); }
    @Transactional(readOnly=true) public List<TechnicianDirectoryResponse> technicianDirectory() {
        return profiles.directoryWithWorkload().stream().map(row->new TechnicianDirectoryResponse(
                ((Number)row[0]).longValue(),String.valueOf(row[1]),String.valueOf(row[2]),Boolean.TRUE.equals(row[3]),
                Math.toIntExact(((Number)row[4]).longValue()),((Number)row[5]).longValue())).toList();
    }
    private List<TechnicianCandidate> candidates() { return eligibleProfiles(profiles.findAll()); }
    private List<TechnicianCandidate> lockedCandidates() { return eligibleProfiles(profiles.findAllForAssignment()); }
    private List<TechnicianCandidate> eligibleProfiles(List<TechnicianProfile> values) {
        return values.stream().filter(p->p.getUser().isActive())
                .filter(p->p.getUser().getRole()==Role.TECHNICIAN||p.getUser().getRole()==Role.AGENT)
                .map(p->new TechnicianCandidate(p,workload(p))).toList();
    }
    private long workload(TechnicianProfile p) { return incidents.countByAssigneeIdAndStatusIn(p.getUser().getId(),ACTIVE); }
}
