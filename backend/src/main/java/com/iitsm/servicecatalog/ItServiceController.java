package com.iitsm.servicecatalog;

import com.iitsm.common.ApiResponse;
import com.iitsm.servicecatalog.dto.ItServiceActiveRequest;
import com.iitsm.servicecatalog.dto.ItServiceRequest;
import com.iitsm.servicecatalog.dto.ItServiceResponse;
import com.iitsm.servicecatalog.service.ItServiceCatalog;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
public class ItServiceController {
    private final ItServiceCatalog catalog;

    public ItServiceController(ItServiceCatalog catalog) { this.catalog = catalog; }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ItServiceResponse>> listAll() { return ApiResponse.ok(catalog.listAll()); }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('EMPLOYEE','TECHNICIAN','ADMIN')")
    public ApiResponse<List<ItServiceResponse>> listActive() { return ApiResponse.ok(catalog.listActive()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ItServiceResponse> get(@PathVariable Long id) { return ApiResponse.ok(catalog.get(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ItServiceResponse> create(@Valid @RequestBody ItServiceRequest request, Authentication authentication) {
        return ApiResponse.ok("IT service created", catalog.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ItServiceResponse> update(@PathVariable Long id, @Valid @RequestBody ItServiceRequest request, Authentication authentication) {
        return ApiResponse.ok("IT service updated", catalog.update(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ItServiceResponse> setActive(@PathVariable Long id, @Valid @RequestBody ItServiceActiveRequest request, Authentication authentication) {
        return ApiResponse.ok("IT service status updated", catalog.setActive(id, request.active(), authentication.getName()));
    }
}
