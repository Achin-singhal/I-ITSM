package com.iitsm.intelligence;

import com.iitsm.common.ApiResponse;
import com.iitsm.intelligence.dto.IncidentSimilarityMatch;
import com.iitsm.intelligence.dto.IncidentSimilarityRequest;
import com.iitsm.intelligence.service.IncidentSimilarityService;
import com.iitsm.incident.service.IncidentService;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/intelligence")
public class IntelligenceController {
    private final IncidentSimilarityService similarity;
    private final IncidentService incidents;
    public IntelligenceController(IncidentSimilarityService similarity,IncidentService incidents) { this.similarity = similarity;this.incidents=incidents; }

    @PostMapping("/duplicates")
    @PreAuthorize("hasAnyRole('EMPLOYEE','TECHNICIAN','ADMIN')")
    public ApiResponse<List<IncidentSimilarityMatch>> duplicates(@Valid @RequestBody IncidentSimilarityRequest request) {
        return ApiResponse.ok("Duplicate suggestions calculated", similarity.duplicates(request));
    }

    @GetMapping("/incidents/{incidentId}/related")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ApiResponse<List<IncidentSimilarityMatch>> related(@PathVariable Long incidentId,Authentication auth) {
        boolean admin=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
        incidents.findVisibleByRole(incidentId,auth.getName(),admin,true);
        return ApiResponse.ok("Related incident suggestions calculated", similarity.related(incidentId));
    }
}
