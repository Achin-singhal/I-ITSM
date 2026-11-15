package com.iitsm.incident;

import com.iitsm.common.ApiResponse;
import com.iitsm.incident.dto.*;
import com.iitsm.incident.entity.Incident;
import com.iitsm.incident.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.incident.dto.IncidentSearchCriteria;
import com.iitsm.incident.service.IncidentSearchService;
import com.iitsm.common.PagedResponse;
import com.iitsm.sla.entity.SlaStatus;
import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.incident.entity.IncidentPriority;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@Validated
public class IncidentController {
    private final IncidentService incidentService;
    private final IncidentSearchService searchService;
    private final UserRepository users;
    public IncidentController(IncidentService incidentService,IncidentSearchService searchService,UserRepository users) { this.incidentService = incidentService;this.searchService=searchService;this.users=users; }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> search(
            @RequestParam(required=false) Long id,@RequestParam(required=false) String query,
            @RequestParam(required=false) IncidentStatus status,@RequestParam(required=false) IncidentPriority priority,
            @RequestParam(required=false) String category,@RequestParam(required=false) Long assigneeId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required=false) SlaStatus slaStatus,@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="25") @Min(1) @Max(100) int size,Authentication authentication) {
        boolean employee=hasRole(authentication,"EMPLOYEE");
        boolean technician=hasRole(authentication,"TECHNICIAN");
        Long reporterId=employee?users.findByEmail(authentication.getName()).map(com.iitsm.user.entity.User::getId).orElse(null):null;
        Long technicianScope=technician?users.findByEmail(authentication.getName()).map(com.iitsm.user.entity.User::getId).orElse(null):null;
        var criteria=new IncidentSearchCriteria(id,query,status,priority,category,assigneeId,createdFrom,createdTo,slaStatus);
        return ResponseEntity.ok(ApiResponse.ok("Incidents searched",searchService.search(criteria,reporterId,technicianScope,page,size)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IncidentResponse>>> getAllIncidents(Authentication authentication) {
        List<IncidentResponse> incidents = incidentService.getIncidentsFor(authentication.getName(), hasRole(authentication,"ADMIN"),hasRole(authentication,"TECHNICIAN")).stream().map(IncidentResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok("Incidents retrieved successfully", incidents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncidentById(@PathVariable Long id, Authentication authentication) {
        Incident incident = incidentService.findVisibleByRole(id,authentication.getName(),hasRole(authentication,"ADMIN"),hasRole(authentication,"TECHNICIAN"));
        return ResponseEntity.ok(ApiResponse.ok(IncidentResponse.from(incident)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','TECHNICIAN','ADMIN')")
    public ResponseEntity<ApiResponse<IncidentResponse>> create(@Valid @RequestBody IncidentCreateRequest request, Authentication authentication) {
        Incident incident = incidentService.createIncident(request.getTitle(), request.getDescription(), request.getCategory(), authentication.getName(),request.getImpact(),request.getUrgency(),request.getBusinessCriticality(),request.getAffectedUserCount(),request.getAffectedService(),request.getServiceCriticality(),request.getServiceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Incident created", IncidentResponse.from(incident)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','TECHNICIAN','ADMIN')")
    public ResponseEntity<ApiResponse<IncidentResponse>> update(@PathVariable Long id, @Valid @RequestBody IncidentUpdateRequest request, Authentication authentication) {
        Incident incident = incidentService.update(id, authentication.getName(), hasRole(authentication,"ADMIN"),hasRole(authentication,"TECHNICIAN"), request.getTitle(), request.getDescription(), request.getCategory(),request.getImpact(),request.getUrgency(),request.getBusinessCriticality(),request.getAffectedUserCount(),request.getAffectedService(),request.getServiceCriticality(),request.getServiceId());
        return ResponseEntity.ok(ApiResponse.ok("Incident updated", IncidentResponse.from(incident)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE','TECHNICIAN','ADMIN')")
    public ResponseEntity<ApiResponse<IncidentResponse>> transition(@PathVariable Long id, @Valid @RequestBody IncidentStatusRequest request,Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Incident status updated", IncidentResponse.from(incidentService.transition(id, request.getStatus(),request.getDetails(),authentication.getName()))));
    }

    @GetMapping("/{id}/activities")
    public ApiResponse<List<IncidentActivityResponse>> activities(@PathVariable Long id,Authentication auth){return ApiResponse.ok(incidentService.activities(id,auth.getName(),hasRole(auth,"ADMIN"),hasRole(auth,"TECHNICIAN")));}

    @PostMapping("/{id}/activities")
    @PreAuthorize("hasAnyRole('EMPLOYEE','TECHNICIAN','ADMIN')")
    public ResponseEntity<ApiResponse<IncidentActivityResponse>> addActivity(@PathVariable Long id,@Valid @RequestBody IncidentActivityRequest request,Authentication auth){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Incident activity recorded",incidentService.addActivity(id,auth.getName(),hasRole(auth,"ADMIN"),hasRole(auth,"TECHNICIAN"),request.getActivityType(),request.getMessage())));}

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
