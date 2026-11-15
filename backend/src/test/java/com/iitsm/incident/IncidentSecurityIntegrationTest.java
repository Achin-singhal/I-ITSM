package com.iitsm.incident;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsm.auth.JwtService;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.entity.Incident;
import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.sla.entity.SlaPolicy;
import com.iitsm.sla.repository.SlaPolicyRepository;
import com.iitsm.user.entity.Role;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.assignment.entity.TechnicianProfile;
import com.iitsm.assignment.repository.TechnicianProfileRepository;
import com.iitsm.assignment.repository.AssignmentRecordRepository;
import com.iitsm.audit.repository.AuditLogRepository;
import com.iitsm.notification.repository.NotificationRepository;
import com.iitsm.notification.service.NotificationPublisher;
import com.iitsm.notification.observer.IncidentEvent;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.incident.category.entity.IncidentCategory;
import com.iitsm.incident.category.repository.IncidentCategoryRepository;
import com.iitsm.sla.repository.SlaHistoryRepository;
import com.iitsm.sla.service.SlaService;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class IncidentSecurityIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired UserRepository users;
    @Autowired JwtService jwt; @Autowired SlaPolicyRepository policies; @Autowired TechnicianProfileRepository profiles;
    @Autowired AuditLogRepository auditLogs; @Autowired NotificationRepository notifications; @Autowired NotificationPublisher publisher; @Autowired IncidentRepository incidents;
    @Autowired AssignmentRecordRepository assignmentRecords; @Autowired SlaHistoryRepository slaHistory; @Autowired SlaService slaService; @Autowired JdbcTemplate jdbc;
    @Autowired IncidentCategoryRepository categories;
    @Autowired PasswordEncoder passwordEncoder;
    @PersistenceContext EntityManager entityManager;
    @BeforeEach void ensureSlaPolicies(){if(policies.count()==0){policies.save(new SlaPolicy("P1 Critical",IncidentPriority.CRITICAL,5,30));policies.save(new SlaPolicy("P2 High",IncidentPriority.HIGH,15,120));policies.save(new SlaPolicy("P3 Medium",IncidentPriority.MEDIUM,60,480));policies.save(new SlaPolicy("P4 Low",IncidentPriority.LOW,240,1440));}if(categories.findByKey("phase4service").isEmpty())categories.saveAndFlush(new IncidentCategory("PHASE4SERVICE"));}
    private String register(String email,String name)throws Exception{
        String body="{\"email\":\""+email+"\",\"password\":\"password123\",\"fullName\":\""+name+"\",\"role\":\"ADMIN\"}";
        String response=mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.role").value("EMPLOYEE")).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("data").path("token").asText();
    }
    private String createIncident(String token)throws Exception{
        String body="{\"title\":\"Payment service outage\",\"description\":\"Payment service is unavailable\",\"category\":\"APPLICATION\",\"impact\":\"EXTENSIVE\",\"urgency\":\"CRITICAL\",\"businessCriticality\":5,\"affectedUserCount\":800,\"affectedService\":\"Payments\",\"serviceCriticality\":5}";
        String response=mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.priority").value("CRITICAL")).andExpect(jsonPath("$.data.priorityScore").exists())
                .andExpect(jsonPath("$.data.priorityExplanation").isNotEmpty()).andExpect(jsonPath("$.data.status").value("CLASSIFIED"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("data").path("id").asText();
    }
    private void deactivateExistingProfiles(){
        var existing=profiles.findAll();
        existing.forEach(profile->profile.configure(false,profile.getExperienceYears(),profile.getMaxActiveWorkload(),profile.getSkills()));
        profiles.saveAll(existing);profiles.flush();
    }
    private void publishConcurrently(IncidentEvent event)throws Exception{
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try{
            Future<?> first=pool.submit(()->{start.await();publisher.publishEvent(event);return null;});
            Future<?> second=pool.submit(()->{start.await();publisher.publishEvent(event);return null;});
            start.countDown();first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
    }
    private void assertAuditEvent(String adminToken,String targetEntity,Long targetId,String action,Long actorId)throws Exception{
        String response=mvc.perform(get("/api/v1/audit").header("Authorization","Bearer "+adminToken)
                        .param("targetEntity",targetEntity).param("targetId",targetId.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        boolean found=false;
        for(var item:mapper.readTree(response).path("data")){
            if(action.equals(item.path("action").asText())){
                org.junit.jupiter.api.Assertions.assertEquals(targetEntity,item.path("targetEntity").asText());
                org.junit.jupiter.api.Assertions.assertEquals(targetId.longValue(),item.path("targetId").asLong());
                org.junit.jupiter.api.Assertions.assertEquals(actorId.longValue(),item.path("actorId").asLong());
                org.junit.jupiter.api.Assertions.assertTrue(item.hasNonNull("timestamp"));
                found=true;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(found,"Expected audit action "+action+" for "+targetEntity+"#"+targetId);
    }
    @Test void publicRegistrationCannotElevateAndOwnershipIsEnforced()throws Exception{
        String first=register("phase2-first@example.test","First Employee");String id=createIncident(first);
        Long incidentId=Long.valueOf(id);
        org.junit.jupiter.api.Assertions.assertTrue(auditLogs.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream().anyMatch(a->a.getAction().equals("CREATED")));
        org.junit.jupiter.api.Assertions.assertEquals(1,notifications.findByRecipientIdOrderByCreatedAtDesc(users.findByEmail("phase2-first@example.test").orElseThrow().getId()).size());
        var event=new IncidentEvent(incidents.findById(incidentId).orElseThrow(),IncidentEvent.EventType.CREATED,"duplicate delivery test");
        publishConcurrently(event);
        org.junit.jupiter.api.Assertions.assertEquals(2,notifications.findByRecipientIdOrderByCreatedAtDesc(users.findByEmail("phase2-first@example.test").orElseThrow().getId()).size());
        mvc.perform(get("/api/v1/sla/incidents/"+id+"/status").header("Authorization","Bearer "+first)).andExpect(status().isOk()).andExpect(jsonPath("$.data.responseDeadline").exists());
        mvc.perform(get("/api/v1/incidents/"+id).header("Authorization","Bearer "+first)).andExpect(status().isOk());
        String second=register("phase2-second@example.test","Second Employee");
        mvc.perform(get("/api/v1/incidents/"+id).header("Authorization","Bearer "+second)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/incidents")).andExpect(status().isUnauthorized());
    }
    @Test void validatesInputsAndRejectsMalformedToken()throws Exception{
        String token=register("phase2-validation@example.test","Validation User");
        mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \"}" )).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/incidents").header("Authorization","Bearer not.a.valid.jwt")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
    }
    @Test void authenticationErrorsDoNotRevealAccountExistenceAndPagingValidationIsClientError()throws Exception{
        String email="phase6-disabled-"+java.util.UUID.randomUUID()+"@example.test";
        User disabled=users.saveAndFlush(new User(email,passwordEncoder.encode("password123"),"Disabled Account",Role.EMPLOYEE));
        disabled.setActive(false);users.saveAndFlush(disabled);
        String unknown=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email","missing-"+email,"password","password123"))))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        String inactive=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email",email,"password","password123"))))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(mapper.readTree(unknown).path("message").asText(),mapper.readTree(inactive).path("message").asText());
        String employee=register("phase6-page-"+java.util.UUID.randomUUID()+"@example.test","Page User");
        mvc.perform(get("/api/v1/incidents/search").header("Authorization","Bearer "+employee).param("size","101"))
                .andExpect(status().isBadRequest());
    }
    @Test void technicianEndpointsHideIncidentsAssignedToOtherTechnicians()throws Exception{
        String employee=register("phase6-scope-employee-"+java.util.UUID.randomUUID()+"@example.test","Scope Employee");
        String id=createIncident(employee);
        User first=users.saveAndFlush(new User("phase6-scope-first-"+java.util.UUID.randomUUID()+"@example.test","unused","First Tech",Role.TECHNICIAN));
        User other=users.saveAndFlush(new User("phase6-scope-other-"+java.util.UUID.randomUUID()+"@example.test","unused","Other Tech",Role.TECHNICIAN));
        profiles.saveAndFlush(new TechnicianProfile(first,true,1,5,Set.of("APPLICATION")));
        profiles.saveAndFlush(new TechnicianProfile(other,true,1,5,Set.of("APPLICATION")));
        Incident target=incidents.findById(Long.valueOf(id)).orElseThrow();target.assignTo(other);incidents.saveAndFlush(target);
        String token=jwt.generateToken(first.getId(),first.getEmail(),first.getRole());
        mvc.perform(get("/api/v1/incidents/"+id).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/sla/incidents/"+id+"/status").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/assignments/"+id+"/history").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/intelligence/incidents/"+id+"/related").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/incidents/search").header("Authorization","Bearer "+token).param("id",id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    }
    @Test void employeeTechnicianAdministratorWorkflowRunsThroughAuthenticatedApis()throws Exception{
        deactivateExistingProfiles();
        String email="phase6-employee-"+java.util.UUID.randomUUID()+"@example.test";
        register(email,"Phase Six Employee");
        String employee=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email",email,"password","password123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
                .andReturn().getResponse().getContentAsString();
        String employeeToken=mapper.readTree(employee).path("data").path("token").asText();
        String incidentId=createIncident(employeeToken);

        User technician=users.saveAndFlush(new User("phase6-technician-"+java.util.UUID.randomUUID()+"@example.test",
                passwordEncoder.encode("password123"),"Phase Six Technician",Role.TECHNICIAN));
        profiles.saveAndFlush(new TechnicianProfile(technician,true,4,5,Set.of("APPLICATION")));
        String technicianToken=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email",technician.getEmail(),"password","password123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("TECHNICIAN"))
                .andReturn().getResponse().getContentAsString();
        technicianToken=mapper.readTree(technicianToken).path("data").path("token").asText();

        User administrator=users.saveAndFlush(new User("phase6-admin-"+java.util.UUID.randomUUID()+"@example.test",
                passwordEncoder.encode("password123"),"Phase Six Administrator",Role.ADMIN));
        String adminToken=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email",administrator.getEmail(),"password","password123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        adminToken=mapper.readTree(adminToken).path("data").path("token").asText();

        mvc.perform(get("/api/v1/assignments/recommendations/"+incidentId+"/ranked").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].technicianId").value(technician.getId()));
        mvc.perform(put("/api/v1/assignments/strategy").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"strategy\":\"SKILL_BASED\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/assignments/technicians/"+technician.getId()+"/profile").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"available\":true,\"experienceYears\":5,\"maxActiveWorkload\":6,\"skills\":[\"APPLICATION\"]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/technicians").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/intelligence/incidents/"+incidentId+"/related").header("Authorization","Bearer "+technicianToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/assignments/auto-route/"+incidentId).header("Authorization","Bearer "+technicianToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.technicianId").value(technician.getId()));
        mvc.perform(patch("/api/v1/incidents/"+incidentId+"/status").header("Authorization","Bearer "+technicianToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACKNOWLEDGED\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/incidents/"+incidentId+"/activities").header("Authorization","Bearer "+technicianToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activityType\":\"INVESTIGATION_NOTE\",\"message\":\"Reproduced and isolated the payment service fault.\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/incidents/"+incidentId+"/activities").header("Authorization","Bearer "+technicianToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].message").exists());
        mvc.perform(get("/api/v1/sla/incidents/"+incidentId+"/status").header("Authorization","Bearer "+technicianToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.responseDeadline").exists());
        mvc.perform(patch("/api/v1/incidents/"+incidentId+"/status").header("Authorization","Bearer "+technicianToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/incidents/"+incidentId+"/status").header("Authorization","Bearer "+technicianToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"details\":\"Payment service restored and verified.\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/incidents/"+incidentId+"/status").header("Authorization","Bearer "+employeeToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/incidents/search").header("Authorization","Bearer "+employeeToken).param("query","Payment"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get("/api/v1/notifications").header("Authorization","Bearer "+employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").exists());

        SlaPolicy lowPolicy=policies.findByPriorityAndActiveTrue(IncidentPriority.LOW).orElseThrow();
        mvc.perform(put("/api/v1/sla/policies/"+lowPolicy.getId()).header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("name",lowPolicy.getName(),"priority","LOW","responseTimeMinutes",lowPolicy.getResponseTimeMinutes(),
                        "resolutionTimeMinutes",lowPolicy.getResolutionTimeMinutes(),"active",true))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/sla/policies").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("name","Duplicate Phase Six Priority","priority","LOW","responseTimeMinutes",61,
                        "resolutionTimeMinutes",601,"active",false))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("The request conflicts with an existing record."));
        mvc.perform(get("/api/v1/audit").header("Authorization","Bearer "+adminToken).param("incidentId",incidentId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
        mvc.perform(get("/api/v1/reports/analytics").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/reports/technicians").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/reports/exports/incidents.csv").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/reports/exports/sla.csv").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/reports/exports/technicians.csv").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/reports/analytics").header("Authorization","Bearer "+employeeToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/audit").header("Authorization","Bearer "+technicianToken)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/assignments/strategy").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"strategy\":\"WORKLOAD_BALANCED\"}")).andExpect(status().isOk());
    }
    @Test void updateAssignmentAndCompleteLifecycleAreEnforced()throws Exception{
        String employee=register("phase2-flow@example.test","Flow Employee");String id=createIncident(employee);
        String update="{\"title\":\"Payment recovered\",\"description\":\"Still intermittent\",\"category\":\"APPLICATION\",\"impact\":\"HIGH\",\"urgency\":\"HIGH\",\"businessCriticality\":4,\"affectedUserCount\":250,\"affectedService\":\"Payments\",\"serviceCriticality\":4}";
        mvc.perform(put("/api/v1/incidents/"+id).header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk());
        User tech=users.save(new User("phase2-tech@example.test","unused-hash","Technician",Role.TECHNICIAN));profiles.save(new TechnicianProfile(tech,true,5,4,Set.of("APPLICATION")));
        String techToken=jwt.generateToken(tech.getId(),tech.getEmail(),tech.getRole());
        mvc.perform(post("/api/v1/assignments/auto-route/"+id).header("Authorization","Bearer "+techToken)).andExpect(status().isOk()).andExpect(jsonPath("$.data.technicianId").value(tech.getId()));
        mvc.perform(patch("/api/v1/incidents/"+id+"/status").header("Authorization","Bearer "+techToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACKNOWLEDGED\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/incidents/"+id+"/status").header("Authorization","Bearer "+techToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/incidents/"+id+"/status").header("Authorization","Bearer "+techToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"details\":\"Payment gateway restarted\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/incidents/"+id+"/status").header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}")).andExpect(status().isOk());
        User admin=users.save(new User("phase2-admin@example.test","unused-hash","Administrator",Role.ADMIN));String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        mvc.perform(patch("/api/v1/incidents/"+id+"/status").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/incidents/"+id+"/status").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/assignments/auto-route/"+id).header("Authorization","Bearer "+adminToken)).andExpect(status().isConflict());
    }
    @Test void loadBasedRecommendationUsesPersistedActiveIncidentCounts()throws Exception{
        String employee=register("phase2-load@example.test","Load Reporter");
        deactivateExistingProfiles();
        User busy=users.save(new User("phase2-busy@example.test","unused","Busy Technician",Role.TECHNICIAN));
        User free=users.save(new User("phase2-free@example.test","unused","Free Technician",Role.TECHNICIAN));
        profiles.save(new TechnicianProfile(busy,true,10,10,Set.of("APPLICATION")));
        profiles.save(new TechnicianProfile(free,true,1,10,Set.of("APPLICATION")));
        String firstId=createIncident(employee);String secondId=createIncident(employee);String targetId=createIncident(employee);
        var first=incidents.findById(Long.valueOf(firstId)).orElseThrow();first.assignTo(busy);incidents.save(first);
        var second=incidents.findById(Long.valueOf(secondId)).orElseThrow();second.assignTo(busy);incidents.save(second);
        String technicianToken=jwt.generateToken(free.getId(),free.getEmail(),free.getRole());
        mvc.perform(get("/api/v1/assignments/recommendations/"+targetId).header("Authorization","Bearer "+technicianToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.technicianId").value(free.getId())).andExpect(jsonPath("$.data.activeWorkload").value(0));
    }

    @Test void administratorCanReassignOnlyWhileIncidentIsAssigned()throws Exception{
        deactivateExistingProfiles();
        String employee=register("phase2-reassign@example.test","Reassignment Reporter");String id=createIncident(employee);
        User first=users.save(new User("phase2-reassign-first@example.test","unused","First Technician",Role.TECHNICIAN));
        User second=users.save(new User("phase2-reassign-second@example.test","unused","Second Technician",Role.TECHNICIAN));
        User admin=users.save(new User("phase2-reassign-admin@example.test","unused","Reassignment Admin",Role.ADMIN));
        TechnicianProfile firstProfile=profiles.save(new TechnicianProfile(first,true,5,5,Set.of("APPLICATION")));
        TechnicianProfile secondProfile=profiles.save(new TechnicianProfile(second,false,4,5,Set.of("APPLICATION")));
        String firstToken=jwt.generateToken(first.getId(),first.getEmail(),first.getRole());
        String secondToken=jwt.generateToken(second.getId(),second.getEmail(),second.getRole());
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());

        mvc.perform(post("/api/v1/assignments/auto-route/"+id).header("Authorization","Bearer "+firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.technicianId").value(first.getId()));
        mvc.perform(post("/api/v1/assignments/auto-route/"+id).header("Authorization","Bearer "+firstToken))
                .andExpect(status().isForbidden());

        firstProfile.configure(false,5,5,Set.of("APPLICATION"));profiles.save(firstProfile);
        secondProfile.configure(true,4,5,Set.of("APPLICATION"));profiles.save(secondProfile);
        mvc.perform(post("/api/v1/assignments/auto-route/"+id).header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.technicianId").value(second.getId()));
        org.junit.jupiter.api.Assertions.assertTrue(assignmentRecords.findByIncidentIdOrderByCreatedAtDesc(Long.valueOf(id)).size()>=2);
        org.junit.jupiter.api.Assertions.assertTrue(auditLogs.findByIncidentIdOrderByCreatedAtDesc(Long.valueOf(id)).stream().anyMatch(a->a.getAction().equals("REASSIGNED")));

        mvc.perform(patch("/api/v1/incidents/"+id+"/status").header("Authorization","Bearer "+secondToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACKNOWLEDGED\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/assignments/auto-route/"+id).header("Authorization","Bearer "+adminToken))
                .andExpect(status().isConflict());
    }

    @Test void legacyOpenIncidentCanBeAssigned()throws Exception{
        deactivateExistingProfiles();
        String employee=register("phase2-legacy-open@example.test","Legacy Reporter");String id=createIncident(employee);
        jdbc.update("UPDATE incidents SET status = 'OPEN' WHERE id = ?",Long.valueOf(id));entityManager.clear();
        User tech=users.save(new User("phase2-legacy-tech@example.test","unused","Legacy Technician",Role.TECHNICIAN));
        profiles.save(new TechnicianProfile(tech,true,3,5,Set.of("APPLICATION")));
        String techToken=jwt.generateToken(tech.getId(),tech.getEmail(),tech.getRole());

        mvc.perform(post("/api/v1/assignments/auto-route/"+id).header("Authorization","Bearer "+techToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.technicianId").value(tech.getId()));
        mvc.perform(get("/api/v1/incidents/"+id).header("Authorization","Bearer "+employee))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }

    @Test void unavailableAndAtCapacityTechniciansAreNotRecommended()throws Exception{
        deactivateExistingProfiles();
        String employee=register("phase2-capacity@example.test","Capacity Reporter");
        User atCapacity=users.save(new User("phase2-at-capacity@example.test","unused","At Capacity",Role.TECHNICIAN));
        User unavailable=users.save(new User("phase2-unavailable@example.test","unused","Unavailable",Role.TECHNICIAN));
        profiles.save(new TechnicianProfile(atCapacity,true,5,1,Set.of("APPLICATION")));
        profiles.save(new TechnicianProfile(unavailable,false,10,10,Set.of("APPLICATION")));
        String activeId=createIncident(employee);String targetId=createIncident(employee);
        var active=incidents.findById(Long.valueOf(activeId)).orElseThrow();active.assignTo(atCapacity);incidents.saveAndFlush(active);
        String techToken=jwt.generateToken(atCapacity.getId(),atCapacity.getEmail(),atCapacity.getRole());

        mvc.perform(get("/api/v1/assignments/recommendations/"+targetId).header("Authorization","Bearer "+techToken))
                .andExpect(status().isConflict());
    }

    @Test void concurrentAssignmentsCannotExceedTechnicianCapacity()throws Exception{
        deactivateExistingProfiles();
        String employee=register("phase2-concurrent-assign@example.test","Concurrent Reporter");
        User technician=users.save(new User("phase2-concurrent-tech@example.test","unused","Single Capacity Technician",Role.TECHNICIAN));
        profiles.save(new TechnicianProfile(technician,true,3,1,Set.of("APPLICATION")));
        String firstId=createIncident(employee);String secondId=createIncident(employee);
        String technicianToken=jwt.generateToken(technician.getId(),technician.getEmail(),technician.getRole());
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try{
            var route=(java.util.concurrent.Callable<Integer>)(()->{
                start.await();
                return mvc.perform(post("/api/v1/assignments/auto-route/"+firstId)
                        .header("Authorization","Bearer "+technicianToken)).andReturn().getResponse().getStatus();
            });
            Future<Integer> first=pool.submit(route);
            Future<Integer> second=pool.submit(()->{
                start.await();
                return mvc.perform(post("/api/v1/assignments/auto-route/"+secondId)
                        .header("Authorization","Bearer "+technicianToken)).andReturn().getResponse().getStatus();
            });
            start.countDown();
            int firstStatus=first.get(15,TimeUnit.SECONDS);int secondStatus=second.get(15,TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertEquals(1,java.util.List.of(firstStatus,secondStatus).stream().filter(code->code==200).count());
            org.junit.jupiter.api.Assertions.assertEquals(1,java.util.List.of(firstStatus,secondStatus).stream().filter(code->code==409).count());
            org.junit.jupiter.api.Assertions.assertEquals(1,incidents.countByAssigneeIdAndStatusIn(technician.getId(),java.util.List.of(IncidentStatus.ASSIGNED)));
        }finally{pool.shutdownNow();}
    }

    @Test void concurrentAssignmentOfSameIncidentCreatesOnlyOneAssignment()throws Exception{
        deactivateExistingProfiles();
        String employee=register("phase2-same-incident-race@example.test","Concurrent Reporter");
        User technician=users.save(new User("phase2-same-incident-tech@example.test","unused","Concurrent Technician",Role.TECHNICIAN));
        profiles.save(new TechnicianProfile(technician,true,3,5,Set.of("APPLICATION")));
        String incidentId=createIncident(employee);
        String token=jwt.generateToken(technician.getId(),technician.getEmail(),technician.getRole());
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try{
            java.util.concurrent.Callable<Integer> route=()->{
                start.await();
                return mvc.perform(post("/api/v1/assignments/auto-route/"+incidentId)
                        .header("Authorization","Bearer "+token)).andReturn().getResponse().getStatus();
            };
            Future<Integer> first=pool.submit(route);Future<Integer> second=pool.submit(route);start.countDown();
            int firstStatus=first.get(15,TimeUnit.SECONDS);int secondStatus=second.get(15,TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertEquals(1,java.util.List.of(firstStatus,secondStatus).stream().filter(code->code==200).count());
            org.junit.jupiter.api.Assertions.assertEquals(1,java.util.List.of(firstStatus,secondStatus).stream().filter(code->code==403).count());
            org.junit.jupiter.api.Assertions.assertEquals(1,assignmentRecords.findByIncidentIdOrderByCreatedAtDesc(Long.valueOf(incidentId)).size());
        }finally{pool.shutdownNow();}
    }

    @Test void concurrentSlaMonitoringPersistsOneWarningAndViolationPerTarget()throws Exception{
        String employee=register("phase2-concurrent-sla@example.test","SLA Reporter");String id=createIncident(employee);Long incidentId=Long.valueOf(id);
        Instant now=Instant.now();Timestamp staleDeadline=Timestamp.from(now.minusSeconds(120));
        jdbc.update("UPDATE sla_tracking SET resolved_at = ? WHERE incident_id <> ?",Timestamp.from(now),incidentId);
        jdbc.update("UPDATE sla_tracking SET response_deadline = ?, resolution_deadline = ?, resolved_at = NULL WHERE incident_id = ?",staleDeadline,staleDeadline,incidentId);
        entityManager.clear();
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try{
            Future<?> first=pool.submit(()->{start.await();slaService.monitorDeadlines();return null;});
            Future<?> second=pool.submit(()->{start.await();slaService.monitorDeadlines();return null;});
            start.countDown();first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}

        var audit= auditLogs.findByIncidentIdOrderByCreatedAtDesc(incidentId);
        org.junit.jupiter.api.Assertions.assertEquals(1,audit.stream().filter(a->a.getAction().equals("SLA_RESPONSE_VIOLATED")).count());
        org.junit.jupiter.api.Assertions.assertEquals(1,audit.stream().filter(a->a.getAction().equals("SLA_RESOLUTION_VIOLATED")).count());
        var history=slaHistory.findByIncidentIdOrderByCreatedAtAsc(incidentId);
        org.junit.jupiter.api.Assertions.assertEquals(1,history.stream().filter(h->h.getAction().equals("SLA_RESPONSE_VIOLATED")).count());
        org.junit.jupiter.api.Assertions.assertEquals(1,history.stream().filter(h->h.getAction().equals("SLA_RESOLUTION_VIOLATED")).count());
        org.junit.jupiter.api.Assertions.assertEquals(1,history.stream().filter(h->h.getAction().equals("SLA_ESCALATED_RESPONSE_BREACH")).count());
        org.junit.jupiter.api.Assertions.assertEquals(1,history.stream().filter(h->h.getAction().equals("SLA_ESCALATED_RESOLUTION_BREACH")).count());
        org.junit.jupiter.api.Assertions.assertEquals(1,audit.stream().filter(a->a.getAction().equals("SLA_ESCALATED_RESPONSE_BREACH")).count());
        org.junit.jupiter.api.Assertions.assertEquals(1,audit.stream().filter(a->a.getAction().equals("SLA_ESCALATED_RESOLUTION_BREACH")).count());
        Long reporterId=users.findByEmail("phase2-concurrent-sla@example.test").orElseThrow().getId();
        var slaNotifications=notifications.findByRecipientIdOrderByCreatedAtDesc(reporterId).stream()
                .filter(n->n.getIncident()!=null&&incidentId.equals(n.getIncident().getId())&&n.getTitle().equals("SLA target violated")).toList();
        org.junit.jupiter.api.Assertions.assertEquals(2,slaNotifications.size());
        var escalationNotifications=notifications.findByRecipientIdOrderByCreatedAtDesc(reporterId).stream()
                .filter(n->n.getIncident()!=null&&incidentId.equals(n.getIncident().getId())&&n.getTitle().equals("SLA incident escalated")).toList();
        org.junit.jupiter.api.Assertions.assertEquals(2,escalationNotifications.size());
    }

    @Test void duplicateSuggestionsAreAvailableToEmployeesButCorrelationsAreRoleRestricted()throws Exception{
        String employee=register("phase4-intelligence@example.test","Intelligence Reporter");
        String incidentId=createIncident(employee);
        String duplicateRequest="{\"title\":\"Payment service outage\",\"description\":\"Payment service is unavailable\",\"category\":\"APPLICATION\",\"affectedService\":\"Payments\"}";
        String duplicateResponse=mvc.perform(post("/api/v1/intelligence/duplicates").header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON).content(duplicateRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var matches=mapper.readTree(duplicateResponse).path("data");
        boolean currentIncidentSuggested=false;
        for(var match:matches) if(match.path("incidentId").asLong()==Long.parseLong(incidentId)
                &&match.path("similarity").asDouble()==1.0&&match.path("title").asText().equals("Payment service outage")) currentIncidentSuggested=true;
        org.junit.jupiter.api.Assertions.assertTrue(currentIncidentSuggested,"The exact existing incident should appear as a duplicate suggestion");
        mvc.perform(get("/api/v1/intelligence/incidents/"+incidentId+"/related").header("Authorization","Bearer "+employee)).andExpect(status().isForbidden());
        User tech=users.save(new User("phase4-intelligence-tech@example.test","unused-hash","Intelligence Technician",Role.TECHNICIAN));
        String techToken=jwt.generateToken(tech.getId(),tech.getEmail(),tech.getRole());
        mvc.perform(get("/api/v1/intelligence/incidents/"+incidentId+"/related").header("Authorization","Bearer "+techToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }

    @Test void ranksTechniciansAndAllowsAdministratorToSelectAnEligibleRecommendation()throws Exception{
        String employee=register("phase4-selected-assignment@example.test","Assignment Reporter");
        String body="{\"title\":\"Phase 4 assignment\",\"description\":\"Specific local service failure\",\"category\":\"PHASE4SERVICE\",\"impact\":\"MODERATE\",\"urgency\":\"NORMAL\",\"businessCriticality\":3,\"affectedUserCount\":1,\"affectedService\":\"Phase Four Service\",\"serviceCriticality\":3}";
        String response=mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=mapper.readTree(response).path("data").path("id").asText();
        User first=users.save(new User("phase4-first-tech@example.test","unused-hash","First Technician",Role.TECHNICIAN));
        User second=users.save(new User("phase4-second-tech@example.test","unused-hash","Second Technician",Role.TECHNICIAN));
        profiles.save(new TechnicianProfile(first,true,4,5,Set.of("PHASE4SERVICE")));
        profiles.save(new TechnicianProfile(second,true,1,5,Set.of("DATABASE")));
        User admin=users.save(new User("phase4-assignment-admin@example.test","unused-hash","Assignment Admin",Role.ADMIN));
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        mvc.perform(get("/api/v1/assignments/recommendations/"+id+"/ranked").header("Authorization","Bearer "+adminToken).param("strategy","SKILL_BASED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].technicianId").value(first.getId()))
                .andExpect(jsonPath("$.data[0].reasons").isArray());
        mvc.perform(post("/api/v1/assignments/"+id+"/assign").header("Authorization","Bearer "+adminToken).param("strategy","SKILL_BASED")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"technicianId\":"+first.getId()+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.technicianId").value(first.getId()));
    }

    @Test void adminUserSearchSupportsNullableBlankMatchingAndPaginatedCount() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        users.saveAndFlush(new User("searchmatch-" + suffix + "@example.test", "unused", "UniqueSearch Alpha " + suffix, Role.EMPLOYEE));
        users.saveAndFlush(new User("UNIQUESEARCH-" + suffix + "@example.test", "unused", "Beta User " + suffix, Role.TECHNICIAN));
        users.saveAndFlush(new User("third-" + suffix + "@example.test", "unused", "UniqueSearch Gamma " + suffix, Role.EMPLOYEE));
        User admin = users.saveAndFlush(new User("search-admin-" + java.util.UUID.randomUUID() + "@example.test", "unused", "Search Admin", Role.ADMIN));
        String auth = "Bearer " + jwt.generateToken(admin.getId(), admin.getEmail(), admin.getRole());
        long expectedAllUsers = users.count();

        mvc.perform(get("/api/v1/admin/users").header("Authorization", auth).param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(expectedAllUsers))
                .andExpect(jsonPath("$.data.totalPages").value((expectedAllUsers + 1) / 2));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", auth).param("query", "").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(expectedAllUsers));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", auth)
                        .param("query", suffix).param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2)).andExpect(jsonPath("$.data.content.length()").value(2));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", auth)
                        .param("query", suffix).param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2)).andExpect(jsonPath("$.data.content.length()").value(1));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", auth)
                        .param("query", "no-such-user-" + suffix).param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0)).andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test void administratorsManageUsersWithoutExposingCredentials() throws Exception {
        String employeeToken=register("phase6-admin-users-employee@example.test","Managed Employee");
        User employee=users.findByEmail("phase6-admin-users-employee@example.test").orElseThrow();
        User admin=users.saveAndFlush(new User("phase6-user-admin@example.test","unused-hash","User Admin",Role.ADMIN));
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        String auth="Bearer "+adminToken;
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+employeeToken))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/active").header("Authorization","Bearer "+employeeToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").header("Authorization",auth).param("query","Managed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].id").value(employee.getId()))
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/admin/users/"+employee.getId()).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value(employee.getEmail()))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/role").header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("TECHNICIAN"));
        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/active").header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email",employee.getEmail(),"password","password123"))))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/active").header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
        mvc.perform(patch("/api/v1/admin/users/"+admin.getId()+"/role").header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/admin/users/"+admin.getId()+"/active").header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isConflict());
        String technicianToken=jwt.generateToken(employee.getId(),employee.getEmail(),Role.TECHNICIAN);
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+technicianToken))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/active").header("Authorization","Bearer "+technicianToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test void administratorsManageCategoriesAndEmployeesCannot() throws Exception {
        User admin=users.saveAndFlush(new User("phase6-category-admin@example.test","unused-hash","Category Admin",Role.ADMIN));
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        String employee=register("phase6-category-employee@example.test","Category Employee");
        String adminAuth="Bearer "+adminToken;
        mvc.perform(post("/api/v1/categories").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Payments\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.active").value(true));
        String response=mvc.perform(get("/api/v1/categories").header("Authorization",adminAuth).param("activeOnly","true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.name == 'Payments')]").exists())
                .andReturn().getResponse().getContentAsString();
        Long id=null; for(var item:mapper.readTree(response).path("data")) if(item.path("name").asText().equals("Payments")) id=item.path("id").asLong();
        org.junit.jupiter.api.Assertions.assertNotNull(id);
        String incidentBody="{\"title\":\"Payment category history\",\"description\":\"Uses managed category\",\"category\":\"Payments\",\"impact\":\"MODERATE\",\"urgency\":\"NORMAL\",\"businessCriticality\":3,\"affectedUserCount\":1,\"affectedService\":\"Payments\",\"serviceCriticality\":3}";
        String incidentResponse=mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON).content(incidentBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long incidentId=mapper.readTree(incidentResponse).path("data").path("id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(id,jdbc.queryForObject("select category_id from incidents where id=?",Long.class,incidentId));
        mvc.perform(put("/api/v1/categories/"+id).header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Payment Services\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Payment Services"));
        mvc.perform(get("/api/v1/incidents/"+incidentId).header("Authorization",adminAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.category").value("Payments"));
        String existingCategoryUpdate="{\"title\":\"Payment category history\",\"description\":\"Updated without changing historical category\",\"category\":\"Payments\",\"impact\":\"MODERATE\",\"urgency\":\"NORMAL\",\"businessCriticality\":3,\"affectedUserCount\":1,\"affectedService\":\"Payments\",\"serviceCriticality\":3}";
        mvc.perform(put("/api/v1/incidents/"+incidentId).header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON).content(existingCategoryUpdate))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.category").value("Payments"));
        mvc.perform(patch("/api/v1/categories/"+id+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        mvc.perform(patch("/api/v1/categories/"+id+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
        mvc.perform(post("/api/v1/categories").header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Forbidden Category\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/categories/"+id).header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Forbidden Rename\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/categories/"+id+"/active").header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
                .andExpect(status().isForbidden());
        String invalidBody="{\"title\":\"Category check\",\"description\":\"Category must be active\",\"category\":\"Payments\",\"impact\":\"MODERATE\",\"urgency\":\"NORMAL\",\"businessCriticality\":3,\"affectedUserCount\":1,\"affectedService\":\"Payments\",\"serviceCriticality\":3}";
        mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+employee).contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test void employeeCanSubmitOneOwnedFeedbackOnlyAfterResolutionAndAdminCanReadIt() throws Exception {
        String employeeToken=register("phase6-feedback-owner@example.test","Feedback Owner");
        String otherToken=register("phase6-feedback-other@example.test","Other Employee");
        String incidentId=createIncident(employeeToken); Long id=Long.valueOf(incidentId);
        mvc.perform(post("/api/v1/incidents/"+id+"/feedback").header("Authorization","Bearer "+employeeToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comments\":\"Great support\"}"))
                .andExpect(status().isConflict());
        User technician=users.saveAndFlush(new User("phase6-feedback-tech@example.test","unused-hash","Feedback Tech",Role.TECHNICIAN));
        var incident=incidents.findById(id).orElseThrow();
        incident.assignTo(technician); incident.acknowledge(technician); incident.transitionTo(IncidentStatus.IN_PROGRESS);
        incident.resolveWith("Service restored"); incidents.saveAndFlush(incident);
        mvc.perform(post("/api/v1/incidents/"+id+"/feedback").header("Authorization","Bearer "+otherToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
                .andExpect(status().isForbidden());
        String technicianToken=jwt.generateToken(technician.getId(),technician.getEmail(),technician.getRole());
        mvc.perform(post("/api/v1/incidents/"+id+"/feedback").header("Authorization","Bearer "+technicianToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/incidents/"+id+"/feedback").header("Authorization","Bearer "+employeeToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comments\":\"Great support\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.rating").value(5));
        mvc.perform(get("/api/v1/incidents/"+id+"/feedback").header("Authorization","Bearer "+employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.comments").value("Great support"));
        mvc.perform(get("/api/v1/incidents/"+id+"/feedback").header("Authorization","Bearer "+otherToken))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/incidents/"+id+"/feedback").header("Authorization","Bearer "+employeeToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/incidents/"+id+"/feedback").header("Authorization","Bearer "+employeeToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":6}"))
                .andExpect(status().isBadRequest());
        User admin=users.saveAndFlush(new User("phase6-feedback-admin@example.test","unused-hash","Feedback Admin",Role.ADMIN));
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        mvc.perform(get("/api/v1/admin/feedback").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[?(@.incidentId == "+id+")].rating").value(org.hamcrest.Matchers.hasItem(5)))
                .andExpect(jsonPath("$.data.content[?(@.employeeEmail == 'phase6-feedback-owner@example.test')].passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/admin/feedback").header("Authorization","Bearer "+employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test void itServiceCatalogEnforcesRoleAccessValidationAndLifecycle() throws Exception {
        String suffix=java.util.UUID.randomUUID().toString().substring(0,8);
        User admin=users.saveAndFlush(new User("catalog-admin-"+suffix+"@example.test","unused-hash","Catalog Admin",Role.ADMIN));
        User technician=users.saveAndFlush(new User("catalog-tech-"+suffix+"@example.test","unused-hash","Catalog Tech",Role.TECHNICIAN));
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        String technicianToken=jwt.generateToken(technician.getId(),technician.getEmail(),technician.getRole());
        String employeeToken=register("catalog-employee-"+suffix+"@example.test","Catalog Employee");
        String adminAuth="Bearer "+adminToken;
        String body=mapper.writeValueAsString(java.util.Map.of("name","Managed Service "+suffix,"description","Primary service description"));

        String created=mvc.perform(post("/api/v1/services").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.active").value(true)).andExpect(jsonPath("$.data.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        Long serviceId=mapper.readTree(created).path("data").path("id").asLong();
        mvc.perform(get("/api/v1/services/"+serviceId).header("Authorization",adminAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Managed Service "+suffix));
        mvc.perform(get("/api/v1/services").header("Authorization",adminAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.id == "+serviceId+")]").exists());
        mvc.perform(get("/api/v1/services/active").header("Authorization","Bearer "+employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.id == "+serviceId+")]").exists());
        mvc.perform(get("/api/v1/services/active").header("Authorization","Bearer "+technicianToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.id == "+serviceId+")]").exists());
        mvc.perform(get("/api/v1/services").header("Authorization","Bearer "+employeeToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/services/"+serviceId).header("Authorization","Bearer "+technicianToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/services").header("Authorization","Bearer "+employeeToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/services").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        String updated=mapper.writeValueAsString(java.util.Map.of("name","Renamed Service "+suffix,"description","Updated description"));
        mvc.perform(put("/api/v1/services/"+serviceId).header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Renamed Service "+suffix))
                .andExpect(jsonPath("$.data.description").value("Updated description"));
        mvc.perform(patch("/api/v1/services/"+serviceId+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        mvc.perform(get("/api/v1/services/active").header("Authorization","Bearer "+employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.id == "+serviceId+")]").doesNotExist());
        mvc.perform(get("/api/v1/services").header("Authorization",adminAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.id == "+serviceId+")].active").value(org.hamcrest.Matchers.hasItem(false)));
        mvc.perform(patch("/api/v1/services/"+serviceId+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
    }

    @Test void incidentCanReferenceOnlyActiveManagedServicesAndLegacyFreeTextStillWorks() throws Exception {
        String suffix=java.util.UUID.randomUUID().toString().substring(0,8);
        User admin=users.saveAndFlush(new User("catalog-incident-admin-"+suffix+"@example.test","unused-hash","Catalog Incident Admin",Role.ADMIN));
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        String employeeToken=register("catalog-incident-employee-"+suffix+"@example.test","Catalog Incident Employee");
        String serviceJson=mapper.writeValueAsString(java.util.Map.of("name","Incident Service "+suffix,"description","For incident tests"));
        String serviceResponse=mvc.perform(post("/api/v1/services").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(serviceJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long serviceId=mapper.readTree(serviceResponse).path("data").path("id").asLong();
        String managedIncident="{\"title\":\"Managed service incident\",\"description\":\"Service selection test\",\"category\":\"APPLICATION\",\"impact\":\"MODERATE\",\"urgency\":\"NORMAL\",\"businessCriticality\":3,\"affectedUserCount\":1,\"serviceCriticality\":3,\"serviceId\":"+serviceId+"}";
        String incidentResponse=mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+employeeToken).contentType(MediaType.APPLICATION_JSON).content(managedIncident))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.serviceId").value(serviceId))
                .andExpect(jsonPath("$.data.serviceName").value("Incident Service "+suffix))
                .andExpect(jsonPath("$.data.affectedService").value("Incident Service "+suffix))
                .andReturn().getResponse().getContentAsString();
        Long incidentId=mapper.readTree(incidentResponse).path("data").path("id").asLong();
        mvc.perform(get("/api/v1/incidents/"+incidentId).header("Authorization","Bearer "+employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.serviceName").value("Incident Service "+suffix));

        String inactiveService=mapper.writeValueAsString(java.util.Map.of("name","Inactive Service "+suffix,"description","Inactive"));
        Long inactiveId=mapper.readTree(mvc.perform(post("/api/v1/services").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(inactiveService))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(patch("/api/v1/services/"+inactiveId+"/active").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        String invalidSelection=managedIncident.replace("\"serviceId\":"+serviceId,"\"serviceId\":"+inactiveId);
        mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+employeeToken).contentType(MediaType.APPLICATION_JSON).content(invalidSelection))
                .andExpect(status().isBadRequest());
        String missingSelection=managedIncident.replace("\"serviceId\":"+serviceId,"\"serviceId\":999999999");
        mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+employeeToken).contentType(MediaType.APPLICATION_JSON).content(missingSelection))
                .andExpect(status().isBadRequest());

        String legacyBody="{\"title\":\"Legacy free text incident\",\"description\":\"Backwards compatibility test\",\"category\":\"APPLICATION\",\"impact\":\"MODERATE\",\"urgency\":\"NORMAL\",\"businessCriticality\":3,\"affectedUserCount\":1,\"affectedService\":\"Legacy free-text service\",\"serviceCriticality\":3}";
        String legacyResponse=mvc.perform(post("/api/v1/incidents").header("Authorization","Bearer "+employeeToken).contentType(MediaType.APPLICATION_JSON).content(legacyBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.serviceId").doesNotExist())
                .andExpect(jsonPath("$.data.serviceName").doesNotExist()).andExpect(jsonPath("$.data.affectedService").value("Legacy free-text service"))
                .andReturn().getResponse().getContentAsString();
        Long legacyId=mapper.readTree(legacyResponse).path("data").path("id").asLong();
        mvc.perform(put("/api/v1/incidents/"+legacyId).header("Authorization","Bearer "+employeeToken).contentType(MediaType.APPLICATION_JSON)
                .content(legacyBody.replace("Legacy free text incident","Updated legacy incident")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.serviceId").doesNotExist())
                .andExpect(jsonPath("$.data.affectedService").value("Legacy free-text service"));
    }

    @Test void administrativeAndFeedbackActionsAreAuditedWithoutLeakingSecretsOrLoggingFailures() throws Exception {
        String suffix=java.util.UUID.randomUUID().toString().substring(0,8);
        User admin=users.saveAndFlush(new User("audit-admin-"+suffix+"@example.test","unused-hash","Audit Admin",Role.ADMIN));
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        String adminAuth="Bearer "+adminToken;
        String employeeToken=register("audit-employee-"+suffix+"@example.test","Audit Employee");
        User employee=users.findByEmail("audit-employee-"+suffix+"@example.test").orElseThrow();
        String feedbackToken=register("audit-feedback-"+suffix+"@example.test","Audit Feedback Employee");
        User feedbackEmployee=users.findByEmail("audit-feedback-"+suffix+"@example.test").orElseThrow();

        mvc.perform(get("/api/v1/audit").header("Authorization","Bearer "+employeeToken)).andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/active").header("Authorization",adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/active").header("Authorization",adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        long afterUserStateChanges=auditLogs.count();
        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/active").header("Authorization",adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(afterUserStateChanges,auditLogs.count());
        mvc.perform(patch("/api/v1/admin/users/"+employee.getId()+"/role").header("Authorization",adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\"}"))
                .andExpect(status().isOk());
        assertAuditEvent(adminToken,"USER",employee.getId(),"USER_CREATED",employee.getId());
        assertAuditEvent(adminToken,"USER",employee.getId(),"USER_DEACTIVATED",admin.getId());
        assertAuditEvent(adminToken,"USER",employee.getId(),"USER_ACTIVATED",admin.getId());
        assertAuditEvent(adminToken,"USER",employee.getId(),"USER_ROLE_CHANGED",admin.getId());

        String serviceName="Audited service "+suffix;
        String serviceBody=mapper.writeValueAsString(java.util.Map.of("name",serviceName,"description","Initial description"));
        Long serviceId=mapper.readTree(mvc.perform(post("/api/v1/services").header("Authorization",adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(serviceBody)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(put("/api/v1/services/"+serviceId).header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("name",serviceName+" Updated","description","Changed description"))))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/services/"+serviceId+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/services/"+serviceId+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        long afterServiceEvents=auditLogs.count();
        mvc.perform(patch("/api/v1/services/"+serviceId+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(afterServiceEvents,auditLogs.count());
        assertAuditEvent(adminToken,"IT_SERVICE",serviceId,"IT_SERVICE_CREATED",admin.getId());
        assertAuditEvent(adminToken,"IT_SERVICE",serviceId,"IT_SERVICE_UPDATED",admin.getId());
        assertAuditEvent(adminToken,"IT_SERVICE",serviceId,"IT_SERVICE_DEACTIVATED",admin.getId());
        assertAuditEvent(adminToken,"IT_SERVICE",serviceId,"IT_SERVICE_ACTIVATED",admin.getId());

        String categoryName="Audited category "+suffix;
        Long categoryId=mapper.readTree(mvc.perform(post("/api/v1/categories").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("name",categoryName))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(put("/api/v1/categories/"+categoryId).header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("name",categoryName+" Updated"))))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/categories/"+categoryId+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/categories/"+categoryId+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        long afterCategoryEvents=auditLogs.count();
        mvc.perform(patch("/api/v1/categories/"+categoryId+"/active").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(afterCategoryEvents,auditLogs.count());
        assertAuditEvent(adminToken,"INCIDENT_CATEGORY",categoryId,"INCIDENT_CATEGORY_CREATED",admin.getId());
        assertAuditEvent(adminToken,"INCIDENT_CATEGORY",categoryId,"INCIDENT_CATEGORY_UPDATED",admin.getId());
        assertAuditEvent(adminToken,"INCIDENT_CATEGORY",categoryId,"INCIDENT_CATEGORY_DEACTIVATED",admin.getId());
        assertAuditEvent(adminToken,"INCIDENT_CATEGORY",categoryId,"INCIDENT_CATEGORY_ACTIVATED",admin.getId());

        String incidentId=createIncident(feedbackToken);
        Long incidentKey=Long.valueOf(incidentId);
        long beforeFailedFeedback=auditLogs.count();
        mvc.perform(post("/api/v1/incidents/"+incidentId+"/feedback").header("Authorization","Bearer "+feedbackToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4,\"comments\":\"must-not-be-audited-before-resolution\"}"))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(beforeFailedFeedback,auditLogs.count());

        User technician=users.saveAndFlush(new User("audit-feedback-tech-"+suffix+"@example.test","unused-hash","Audit Feedback Tech",Role.TECHNICIAN));
        technician.setRole(Role.TECHNICIAN);
        var incident=incidents.findById(incidentKey).orElseThrow();
        incident.assignTo(technician); incident.acknowledge(technician); incident.transitionTo(IncidentStatus.IN_PROGRESS);
        incident.resolveWith("Audit workflow resolution"); incidents.saveAndFlush(incident);
        String commentMarker="private-feedback-text-"+suffix;
        String feedbackJson=mvc.perform(post("/api/v1/incidents/"+incidentId+"/feedback").header("Authorization","Bearer "+feedbackToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(java.util.Map.of("rating",5,"comments",commentMarker))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long feedbackId=mapper.readTree(feedbackJson).path("data").path("id").asLong();
        long afterFeedback=auditLogs.count();
        mvc.perform(post("/api/v1/incidents/"+incidentId+"/feedback").header("Authorization","Bearer "+feedbackToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(java.util.Map.of("rating",3,"comments","duplicate"))))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(afterFeedback,auditLogs.count());
        assertAuditEvent(adminToken,"INCIDENT_FEEDBACK",feedbackId,"FEEDBACK_SUBMITTED",feedbackEmployee.getId());
        String feedbackAudit=mvc.perform(get("/api/v1/audit").header("Authorization",adminAuth)
                        .param("targetEntity","INCIDENT_FEEDBACK").param("targetId",feedbackId.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(feedbackAudit.contains(commentMarker));
        org.junit.jupiter.api.Assertions.assertFalse(feedbackAudit.toLowerCase(java.util.Locale.ROOT).contains("passwordhash"));

        long beforeFailedService=auditLogs.count();
        mvc.perform(post("/api/v1/services").header("Authorization",adminAuth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(beforeFailedService,auditLogs.count());
        mvc.perform(post("/api/v1/services").header("Authorization","Bearer "+feedbackToken).contentType(MediaType.APPLICATION_JSON).content(serviceBody))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(beforeFailedService,auditLogs.count());
    }
}
