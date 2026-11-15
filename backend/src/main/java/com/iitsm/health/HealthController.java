package com.iitsm.health;

import com.iitsm.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import com.iitsm.auth.JwtService;
import com.iitsm.incident.service.IncidentService;
import com.iitsm.assignment.service.AssignmentService;
import com.iitsm.sla.service.SlaService;
import com.iitsm.notification.service.NotificationPublisher;
import com.iitsm.audit.service.AuditService;
import com.iitsm.intelligence.service.IncidentSimilarityService;
import com.iitsm.report.service.ReportService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check and module registration diagnostics controller.
 * Accessible without authentication to verify deployment status.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final ApplicationContext context;
    private final Environment environment;
    public HealthController(ApplicationContext context,Environment environment){this.context=context;this.environment=environment;}

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> healthData = new LinkedHashMap<>();
        healthData.put("status", "UP");
        healthData.put("application", "I-ITSM Backend");
        healthData.put("version", "1.0.0");
        healthData.put("environment", String.join(",",environment.getActiveProfiles()));
        healthData.put("timestamp", Instant.now().toString());

        Map<String, String> modules = new LinkedHashMap<>();
        modules.put("auth", beanStatus(JwtService.class));
        modules.put("user", beanStatus(com.iitsm.user.repository.UserRepository.class));
        modules.put("incident", beanStatus(IncidentService.class));
        modules.put("assignment", beanStatus(AssignmentService.class));
        modules.put("sla", beanStatus(SlaService.class));
        modules.put("notification", beanStatus(NotificationPublisher.class));
        modules.put("audit", beanStatus(AuditService.class));
        modules.put("intelligence", beanStatus(IncidentSimilarityService.class));
        modules.put("report", beanStatus(ReportService.class));

        healthData.put("modules", modules);

        return ResponseEntity.ok(ApiResponse.ok("I-ITSM backend service is healthy", healthData));
    }
    private String beanStatus(Class<?> type){return context.getBeanNamesForType(type).length>0?"INITIALIZED":"UNAVAILABLE";}
}
