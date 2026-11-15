package com.iitsm.incident.category;

import com.iitsm.common.ApiResponse;
import com.iitsm.incident.category.dto.IncidentCategoryActiveRequest;
import com.iitsm.incident.category.dto.IncidentCategoryRequest;
import com.iitsm.incident.category.dto.IncidentCategoryResponse;
import com.iitsm.incident.category.service.IncidentCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class IncidentCategoryController {
    private final IncidentCategoryService service;
    public IncidentCategoryController(IncidentCategoryService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<IncidentCategoryResponse>> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.ok(service.list(activeOnly));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IncidentCategoryResponse> create(@Valid @RequestBody IncidentCategoryRequest request, Authentication authentication) {
        return ApiResponse.ok("Category created", service.create(request, authentication.getName()));
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IncidentCategoryResponse> update(@PathVariable Long id, @Valid @RequestBody IncidentCategoryRequest request, Authentication authentication) {
        return ApiResponse.ok("Category updated", service.update(id, request, authentication.getName()));
    }
    @PatchMapping("/{id}/active") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IncidentCategoryResponse> setActive(@PathVariable Long id, @Valid @RequestBody IncidentCategoryActiveRequest request, Authentication authentication) {
        return ApiResponse.ok("Category status updated", service.setActive(id, request.active(), authentication.getName()));
    }
}
