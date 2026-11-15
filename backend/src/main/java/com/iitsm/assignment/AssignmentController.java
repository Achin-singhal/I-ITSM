package com.iitsm.assignment;

import com.iitsm.assignment.dto.*;
import com.iitsm.assignment.service.AssignmentService;
import com.iitsm.incident.service.IncidentService;
import com.iitsm.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/assignments")
public class AssignmentController {
    private final AssignmentService service;
    private final IncidentService incidents;
    public AssignmentController(AssignmentService service,IncidentService incidents){this.service=service;this.incidents=incidents;}
    @GetMapping("/recommendations/{incidentId}") @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ApiResponse<TechnicianRecommendation> recommend(@PathVariable Long incidentId,@RequestParam(required=false) String strategy,Authentication auth){checkVisibility(incidentId,auth);return ApiResponse.ok(service.recommend(incidentId,strategy));}
    @GetMapping("/recommendations/{incidentId}/ranked") @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ApiResponse<List<RankedTechnicianRecommendation>> ranked(@PathVariable Long incidentId,@RequestParam(required=false) String strategy,Authentication auth){checkVisibility(incidentId,auth);return ApiResponse.ok(service.rankedRecommendations(incidentId,strategy));}
    @PostMapping("/auto-route/{incidentId}") @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ApiResponse<TechnicianRecommendation> autoRoute(@PathVariable Long incidentId,Authentication auth,@RequestParam(required=false) String strategy){return ApiResponse.ok("Incident routed",service.assign(incidentId,auth.getName(),strategy));}
    @PostMapping("/{incidentId}/assign") @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ApiResponse<TechnicianRecommendation> assignSelected(@PathVariable Long incidentId,@Valid @RequestBody AssignmentSelectionRequest request,Authentication auth,@RequestParam(required=false) String strategy){return ApiResponse.ok("Incident assigned to selected technician",service.assignSelected(incidentId,auth.getName(),strategy,request.technicianId()));}
    @PutMapping("/strategy") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> setStrategy(@Valid @RequestBody AssignmentStrategyRequest request){return ApiResponse.ok("Assignment strategy configured",service.configureStrategy(request.getStrategy()));}
    @PutMapping("/technicians/{userId}/profile") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TechnicianRecommendation> configureProfile(@PathVariable Long userId,@Valid @RequestBody TechnicianProfileRequest request){
        var p=service.configureProfile(userId,request.isAvailable(),request.getExperienceYears(),request.getMaxActiveWorkload(),request.getSkills());
        return ApiResponse.ok("Technician profile configured",TechnicianRecommendation.of(p,0,"PROFILE"));
    }
    @GetMapping("/{incidentId}/history") @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ApiResponse<List<AssignmentRecordResponse>> history(@PathVariable Long incidentId,Authentication auth){checkVisibility(incidentId,auth);return ApiResponse.ok(service.history(incidentId).stream().map(AssignmentRecordResponse::from).toList());}
    private void checkVisibility(Long incidentId,Authentication auth){
        boolean admin=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
        boolean technician=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_TECHNICIAN"));
        incidents.findVisibleByRole(incidentId,auth.getName(),admin,technician);
    }
}
