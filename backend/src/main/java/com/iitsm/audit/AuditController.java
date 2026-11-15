package com.iitsm.audit;

import com.iitsm.audit.dto.AuditResponse;
import com.iitsm.audit.service.AuditQueryService;
import com.iitsm.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController @RequestMapping("/api/v1/audit") @PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final AuditQueryService service;
    public AuditController(AuditQueryService service) { this.service=service; }
    @GetMapping public ApiResponse<List<AuditResponse>> search(@RequestParam(required=false) Long incidentId,
            @RequestParam(required=false) Long actorId,@RequestParam(required=false) String action,
            @RequestParam(required=false) String targetEntity,@RequestParam(required=false) Long targetId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.ok(service.search(incidentId,actorId,action,targetEntity,targetId,from,to));
    }
}
