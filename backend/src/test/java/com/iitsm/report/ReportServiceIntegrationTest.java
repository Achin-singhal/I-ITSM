package com.iitsm.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsm.assignment.entity.TechnicianProfile;
import com.iitsm.assignment.repository.TechnicianProfileRepository;
import com.iitsm.auth.JwtService;
import com.iitsm.incident.entity.Incident;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.incident.dto.IncidentSearchCriteria;
import com.iitsm.report.service.ReportExportService;
import com.iitsm.report.service.ReportService;
import com.iitsm.sla.entity.SlaPolicy;
import com.iitsm.sla.entity.SlaTracking;
import com.iitsm.sla.repository.SlaPolicyRepository;
import com.iitsm.sla.repository.SlaTrackingRepository;
import com.iitsm.user.entity.Role;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ReportServiceIntegrationTest {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired IncidentRepository incidents;
    @Autowired SlaTrackingRepository tracking; @Autowired SlaPolicyRepository policies;
    @Autowired TechnicianProfileRepository technicianProfiles; @Autowired ReportService reports;
    @Autowired ReportExportService exports; @Autowired JwtService jwt; @Autowired JdbcTemplate jdbc;

    private User employee;
    private Instant from;
    private Instant to;

    @BeforeEach void setup() {
        employee=users.saveAndFlush(new User("phase5-employee-"+java.util.UUID.randomUUID()+"@example.test","not-a-password","Phase Five Employee",Role.EMPLOYEE));
        from=Instant.now().plusSeconds(1_000L+java.util.concurrent.ThreadLocalRandom.current().nextLong(1_000_000_000L))
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);to=from.plusSeconds(86_400);
        if(policies.findByPriorityAndActiveTrue(IncidentPriority.CRITICAL).isEmpty())
            policies.saveAndFlush(new SlaPolicy("Phase 5 Critical",IncidentPriority.CRITICAL,5,30));
    }

    @Test void analyticsUseDateFilteredDatabaseCountsAndSlaCompliance() {
        Incident incident=saveIncident(employee,null,"=FORMULA incident","NETWORK",IncidentStatus.IN_PROGRESS,from.plusSeconds(3600),null);
        SlaPolicy policy=policies.findByPriorityAndActiveTrue(IncidentPriority.CRITICAL).orElseThrow();
        SlaTracking item=tracking.saveAndFlush(new SlaTracking(incident,policy));
        item.setResponseWarningSent();item.recordResponseViolation();tracking.saveAndFlush(item);

        var summary=reports.getSummary(from,to);
        assertEquals(1,summary.totalIncidents());assertEquals(1,summary.inProgressIncidents());
        assertEquals(1,summary.criticalHighIncidents());assertEquals(1,summary.slaWarnings());assertEquals(1,summary.slaBreaches());
        assertEquals(50.0,summary.slaCompliancePercentage());assertNull(summary.averageResolutionSeconds());
        var analytics=reports.getAnalytics(from,to);
        assertEquals(1L,analytics.incidentsByStatus().get("IN_PROGRESS"));
        assertEquals(1L,analytics.incidentsByCategory().get("NETWORK"));
        java.sql.Date databaseBucket=jdbc.queryForObject("select cast(created_at as date) from incidents where id=?",java.sql.Date.class,incident.getId());
        assertEquals(1,analytics.createdOverTime().size());assertEquals(databaseBucket.toLocalDate(),analytics.createdOverTime().get(0).date());
        assertEquals(1,analytics.unresolvedIncidents());

        jdbc.update("update incidents set status='RESOLVED', resolved_at=? where id=?",Timestamp.from(from.plusSeconds(3600+900)),incident.getId());
        assertEquals(900.0,reports.getSummary(from,to).averageResolutionSeconds());
        assertEquals(1,reports.getAnalytics(from,to).resolvedIncidents());
    }

    @Test void reportQueriesAcceptNullableInstantBoundsAndKeepExclusiveEndSemantics() {
        long incidentsBefore = incidents.count();
        Instant anchor = Instant.parse("2099-06-01T00:00:00Z");
        Instant end = anchor.plusSeconds(200);
        User technician = users.saveAndFlush(new User("phase5-null-date-tech-" + java.util.UUID.randomUUID()
                + "@example.test", "hash", "Nullable Date Technician", Role.TECHNICIAN));
        technicianProfiles.saveAndFlush(new TechnicianProfile(technician, true, 2, 5, Set.of("NETWORK")));
        Incident beforeStart = saveIncident(employee, technician, "Before nullable date start", "NETWORK", IncidentStatus.OPEN,
                anchor.minusSeconds(100), null);
        saveIncident(employee, technician, "At nullable date start", "NETWORK", IncidentStatus.OPEN,
                anchor, null);
        saveIncident(employee, technician, "Inside nullable date bounds", "NETWORK", IncidentStatus.IN_PROGRESS,
                anchor.plusSeconds(100), null);
        saveIncident(employee, technician, "At exclusive nullable date end", "NETWORK", IncidentStatus.RESOLVED,
                end, end.plusSeconds(30));
        SlaPolicy policy = policies.findByPriorityAndActiveTrue(IncidentPriority.CRITICAL).orElseThrow();
        tracking.saveAndFlush(new SlaTracking(beforeStart, policy));

        assertEquals(incidentsBefore + 4, reports.getSummary(null, null).totalIncidents());
        assertEquals(incidentsBefore + 4, reports.getAnalytics(null, null).summary().totalIncidents());
        var slaPage = tracking.findReportPage(null, null, org.springframework.data.domain.PageRequest.of(0, 1));
        assertEquals(tracking.count(), slaPage.getTotalElements());

        assertEquals(incidentsBefore + 4, incidents.countCreatedInRange(null, null));
        assertEquals(3, incidents.countCreatedInRange(anchor, null));
        assertEquals(incidentsBefore + 3, incidents.countCreatedInRange(null, end));
        assertEquals(2, incidents.countCreatedInRange(anchor, end));

        var allTechnicianWork = reports.getTechnicianReport(technician.getId(), null, null).orElseThrow();
        var fromOnly = reports.getTechnicianReport(technician.getId(), anchor, null).orElseThrow();
        var toOnly = reports.getTechnicianReport(technician.getId(), null, end).orElseThrow();
        var bounded = reports.getTechnicianReport(technician.getId(), anchor, end).orElseThrow();
        assertEquals(4, allTechnicianWork.assignedIncidentCount());
        assertEquals(3, fromOnly.assignedIncidentCount());
        assertEquals(3, toOnly.assignedIncidentCount());
        assertEquals(2, bounded.assignedIncidentCount());
    }

    @Test void incidentSearchPaginatesFiltersAndKeepsEmployeeOwnershipInsideTheQuery() {
        saveIncident(employee,null,"VPN access outage","NETWORK",IncidentStatus.OPEN,from.plusSeconds(100),null);
        User other=users.saveAndFlush(new User("phase5-other-"+java.util.UUID.randomUUID()+"@example.test","hash","Other Employee",Role.EMPLOYEE));
        saveIncident(other,null,"VPN access issue","NETWORK",IncidentStatus.OPEN,from.plusSeconds(200),null);
        var criteria=new IncidentSearchCriteria(null,"VPN",IncidentStatus.OPEN,null,"NETWORK",null,from,to,null);
        var first=reportsSearch(criteria,employee.getId(),0,1);
        assertEquals(1,first.totalElements());assertEquals(1,first.content().size());assertEquals(1,first.totalPages());
        var noMatch=reportsSearch(new IncidentSearchCriteria(null,"printer",null,null,null,null,from,to,null),employee.getId(),0,10);
        assertEquals(0,noMatch.totalElements());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->reportsSearch(criteria,employee.getId(),0,101));
    }

    @Test void incidentSearchCategoryFilterSupportsNullAndCaseInsensitiveValues() {
        Incident network = saveIncident(employee, null, "Network incident", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(100), null);
        saveIncident(employee, null, "Software incident", "SOFTWARE", IncidentStatus.OPEN, from.plusSeconds(200), null);

        var noCategoryFilter = reportsSearch(new IncidentSearchCriteria(null, null, null, null, null, null, null, null, null), employee.getId(), 0, 1);
        assertEquals(2, noCategoryFilter.totalElements());

        var matchingCategory = reportsSearch(new IncidentSearchCriteria(null, null, null, null, "network", null, null, null, null), employee.getId(), 0, 1);
        assertEquals(1, matchingCategory.totalElements());
        assertEquals(network.getId(), matchingCategory.content().get(0).id());

        var unmatchedCategory = reportsSearch(new IncidentSearchCriteria(null, null, null, null, "CLOUD", null, null, null, null), employee.getId(), 0, 10);
        assertEquals(0, unmatchedCategory.totalElements());
    }

    @Test void incidentSearchWithNullQueryReturnsUnfilteredResultsAndCorrectCount() {
        saveIncident(employee, null, "Null query first", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(100), null);
        saveIncident(employee, null, "Null query second", "SOFTWARE", IncidentStatus.OPEN, from.plusSeconds(200), null);

        var firstPage = reportsSearch(searchCriteria(null), employee.getId(), 0, 1);
        assertEquals(2, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertEquals(1, firstPage.content().size());
        assertEquals(0, firstPage.page());

        var secondPage = reportsSearch(searchCriteria(null), employee.getId(), 1, 1);
        assertEquals(2, secondPage.totalElements());
        assertEquals(1, secondPage.content().size());
        assertEquals(1, secondPage.page());
    }

    @Test void incidentSearchMatchesTicketNumberCaseInsensitively() {
        Incident incident = saveIncident(employee, null, "Ticket search target", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(100), null);
        String ticketPart = incident.getTicketNumber().substring(4, 10).toLowerCase(java.util.Locale.ROOT);

        var result = reportsSearch(searchCriteria(ticketPart), employee.getId(), 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals(incident.getId(), result.content().get(0).id());
    }

    @Test void incidentSearchMatchesTitleCaseInsensitively() {
        Incident incident = saveIncident(employee, null, "Unusual Printer Jam", "HARDWARE", IncidentStatus.OPEN, from.plusSeconds(100), null);

        var result = reportsSearch(searchCriteria("pRiNtEr jAm"), employee.getId(), 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals(incident.getId(), result.content().get(0).id());
    }

    @Test void incidentSearchMatchesDescriptionCaseInsensitively() {
        Incident incident = new Incident("Description search target", "UniqueOrchid phrase in the description", IncidentPriority.CRITICAL, "NETWORK", employee);
        incident = incidents.saveAndFlush(incident);

        var result = reportsSearch(searchCriteria("uniqueorchid"), employee.getId(), 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals(incident.getId(), result.content().get(0).id());
    }

    @Test void incidentSearchReturnsNoResultAndZeroCountForUnmatchedQuery() {
        saveIncident(employee, null, "Existing incident", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(100), null);

        var result = reportsSearch(searchCriteria("no-such-incident-term"), employee.getId(), 0, 10);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test void incidentSearchDateFiltersAllowNullBounds() {
        saveIncident(employee, null, "Before date window", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(100), null);
        saveIncident(employee, null, "Inside date window", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(200), null);
        saveIncident(employee, null, "After date window", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(300), null);

        var result = reportsSearch(searchCriteria(null, null, null), employee.getId(), 0, 10);

        assertEquals(3, result.totalElements());
        assertEquals(3, result.content().size());
    }

    @Test void incidentSearchDateFilterSupportsCreatedFromOnly() {
        saveIncident(employee, null, "Before from bound", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(100), null);
        Incident atBound = saveIncident(employee, null, "At from bound", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(200), null);
        saveIncident(employee, null, "After from bound", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(300), null);

        var result = reportsSearch(searchCriteria(from.plusSeconds(200), null), employee.getId(), 0, 10);

        assertEquals(2, result.totalElements());
        assertTrue(result.content().stream().anyMatch(item -> item.id().equals(atBound.getId())));
    }

    @Test void incidentSearchDateFilterSupportsCreatedToOnly() {
        Incident beforeBound = saveIncident(employee, null, "Before to bound", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(100), null);
        saveIncident(employee, null, "At to bound", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(200), null);
        saveIncident(employee, null, "After to bound", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(300), null);

        var result = reportsSearch(searchCriteria(null, from.plusSeconds(200)), employee.getId(), 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals(beforeBound.getId(), result.content().get(0).id());
    }

    @Test void incidentSearchDateFilterCombinesCreatedFromAndCreatedTo() {
        saveIncident(employee, null, "Before combined bounds", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(100), null);
        Incident included = saveIncident(employee, null, "Inside combined bounds", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(200), null);
        saveIncident(employee, null, "At exclusive upper bound", "NETWORK", IncidentStatus.OPEN, from.plusSeconds(300), null);

        var criteria = new IncidentSearchCriteria(null, null, null, null, null, null,
                from.plusSeconds(150), from.plusSeconds(300), null);
        var result = reportsSearch(criteria, employee.getId(), 0, 10);

        assertEquals(1, result.totalElements(), result.content().stream().map(com.iitsm.incident.dto.IncidentResponse::title).toList().toString());
        assertEquals(included.getId(), result.content().get(0).id());
    }

    private IncidentSearchCriteria searchCriteria(String query) {
        return new IncidentSearchCriteria(null, query, null, null, null, null, null, null, null);
    }

    private IncidentSearchCriteria searchCriteria(Instant createdFrom, Instant createdTo) {
        return searchCriteria(null, createdFrom, createdTo);
    }

    private IncidentSearchCriteria searchCriteria(String query, Instant createdFrom, Instant createdTo) {
        return new IncidentSearchCriteria(null, query, null, null, null, null, createdFrom, createdTo, null);
    }

    @Test void incidentSearchApiAppliesEmployeeOwnershipAndReturnsPaginationMetadata() throws Exception {
        saveIncident(employee,null,"Owned outage","NETWORK",IncidentStatus.OPEN,from.plusSeconds(100),null);
        User other=users.saveAndFlush(new User("phase5-api-other-"+java.util.UUID.randomUUID()+"@example.test","hash","Other Employee",Role.EMPLOYEE));
        saveIncident(other,null,"Private outage","NETWORK",IncidentStatus.OPEN,from.plusSeconds(200),null);
        String token=jwt.generateToken(employee.getId(),employee.getEmail(),employee.getRole());
        mvc.perform(get("/api/v1/incidents/search").header("Authorization","Bearer "+token).param("query","outage").param("page","0").param("size","1"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.totalElements").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.content[0].reporterId").value(employee.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.page").value(0));
    }

    @Test void technicianWorkloadAggregationsIncludeResolutionTimeAndDirectoryData() {
        User tech=users.saveAndFlush(new User("phase5-tech-"+java.util.UUID.randomUUID()+"@example.test","hash","Phase Five Technician",Role.TECHNICIAN));
        technicianProfiles.saveAndFlush(new TechnicianProfile(tech,true,6,8,Set.of("NETWORK")));
        Incident resolved=saveIncident(employee,tech,"Resolved network request","NETWORK",IncidentStatus.RESOLVED,from.plusSeconds(1000),from.plusSeconds(1900));
        saveIncident(employee,tech,"Active network request","NETWORK",IncidentStatus.IN_PROGRESS,from.plusSeconds(2000),null);
        var rows=reports.getTechnicianReports(from,to);
        var mine=rows.stream().filter(row->row.technicianId().equals(tech.getId())).findFirst().orElseThrow();
        assertEquals(2,mine.assignedIncidentCount());assertEquals(1,mine.activeWorkload());assertEquals(1,mine.resolvedIncidentCount());
        assertEquals(900.0,mine.averageResolutionSeconds());
        assertTrue(reports.technicianDirectory().stream().anyMatch(row->row.userId().equals(tech.getId())&&row.activeWorkload()==1));
    }

    @Test void csvEscapesCellsAndReportEndpointsEnforceAdminAuthorization() throws Exception {
        saveIncident(employee,null,"=1+1","NETWORK",IncidentStatus.OPEN,from.plusSeconds(100),null);
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        exports.writeIncidents(new IncidentSearchCriteria(null,"=1+1",null,null,null,null,from,to,null),output);
        String csv=output.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\"id\",\"ticket_number\""));assertTrue(csv.contains("\"'=1+1\""));

        String employeeToken=jwt.generateToken(employee.getId(),employee.getEmail(),employee.getRole());
        mvc.perform(get("/api/v1/reports/analytics").header("Authorization","Bearer "+employeeToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reports/exports/incidents.csv").header("Authorization","Bearer "+employeeToken)).andExpect(status().isForbidden());
        User admin=users.saveAndFlush(new User("phase5-admin-"+java.util.UUID.randomUUID()+"@example.test","hash","Phase Five Admin",Role.ADMIN));
        String adminToken=jwt.generateToken(admin.getId(),admin.getEmail(),admin.getRole());
        mvc.perform(get("/api/v1/reports/analytics").header("Authorization","Bearer "+adminToken).param("from",from.toString()).param("to",to.toString()))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.summary.totalIncidents").value(1));
        mvc.perform(get("/api/v1/reports/exports/incidents.csv").header("Authorization","Bearer "+adminToken).param("createdFrom",from.toString()).param("createdTo",to.toString()))
                .andExpect(status().isOk());
    }

    private com.iitsm.common.PagedResponse<com.iitsm.incident.dto.IncidentResponse> reportsSearch(com.iitsm.incident.dto.IncidentSearchCriteria c,Long owner,int page,int size){
        return new com.iitsm.incident.service.IncidentSearchService(incidents).search(c,owner,null,page,size);
    }
    private Incident saveIncident(User reporter,User assignee,String title,String category,IncidentStatus status,Instant created,Instant resolved) {
        Incident incident=new Incident(title,"Description for "+title,IncidentPriority.CRITICAL,category,reporter);
        incident.setAssignee(assignee);incident=incidents.saveAndFlush(incident);
        jdbc.update("update incidents set status=?,created_at=?,updated_at=?,resolved_at=? where id=?",status.name(),Timestamp.from(created),Timestamp.from(created),resolved==null?null:Timestamp.from(resolved),incident.getId());
        return incidents.findById(incident.getId()).orElseThrow();
    }
}
