package com.iitsm.sla;

import com.iitsm.common.ApiResponse;
import com.iitsm.incident.service.IncidentService;
import com.iitsm.sla.dto.*;
import com.iitsm.sla.service.SlaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/sla")
public class SlaController {
    private final SlaService service; private final IncidentService incidents;
    public SlaController(SlaService service,IncidentService incidents){this.service=service;this.incidents=incidents;}
    @GetMapping("/policies") public ApiResponse<List<SlaPolicyResponse>> policies(){return ApiResponse.ok(service.listPolicies());}
    @PostMapping("/policies") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SlaPolicyResponse> create(@Valid @RequestBody SlaPolicyRequest request){return ApiResponse.ok("SLA policy created",service.savePolicy(request,null));}
    @PutMapping("/policies/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SlaPolicyResponse> update(@PathVariable Long id,@Valid @RequestBody SlaPolicyRequest request){return ApiResponse.ok("SLA policy updated",service.savePolicy(request,id));}
    @DeleteMapping("/policies/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deactivate(@PathVariable Long id){service.deactivatePolicy(id);return ApiResponse.ok("SLA policy deactivated",null);}
    @GetMapping("/incidents/{incidentId}/status")
    public ApiResponse<SlaStatusResponse> status(@PathVariable Long incidentId,Authentication auth){
        boolean admin=hasRole(auth,"ADMIN"),technician=hasRole(auth,"TECHNICIAN");
        incidents.findVisibleByRole(incidentId,auth.getName(),admin,technician);
        return ApiResponse.ok(service.status(incidentId));
    }
    @GetMapping("/incidents/{incidentId}/history")
    public ApiResponse<List<SlaHistoryResponse>> history(@PathVariable Long incidentId,Authentication auth){
        boolean admin=hasRole(auth,"ADMIN"),technician=hasRole(auth,"TECHNICIAN");
        incidents.findVisibleByRole(incidentId,auth.getName(),admin,technician);return ApiResponse.ok(service.history(incidentId));
    }
    private boolean hasRole(Authentication auth,String role){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+role));}
}
