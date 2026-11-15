package com.iitsm.incident.feedback;

import com.iitsm.common.ApiResponse;
import com.iitsm.common.PagedResponse;
import com.iitsm.incident.feedback.dto.IncidentFeedbackRequest;
import com.iitsm.incident.feedback.dto.IncidentFeedbackResponse;
import com.iitsm.incident.feedback.service.IncidentFeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1") @Validated
public class IncidentFeedbackController {
    private final IncidentFeedbackService service;
    public IncidentFeedbackController(IncidentFeedbackService service) { this.service = service; }

    @PostMapping("/incidents/{id}/feedback") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<IncidentFeedbackResponse> submit(@PathVariable Long id, @Valid @RequestBody IncidentFeedbackRequest request, Authentication auth) {
        return ApiResponse.ok("Feedback submitted", service.submit(id, auth.getName(), request));
    }

    @GetMapping("/incidents/{id}/feedback") @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<IncidentFeedbackResponse> mine(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok(service.mine(id, auth.getName()));
    }

    @GetMapping("/admin/feedback") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PagedResponse<IncidentFeedbackResponse>> all(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(PagedResponse.from(service.all(page, size)));
    }
}
