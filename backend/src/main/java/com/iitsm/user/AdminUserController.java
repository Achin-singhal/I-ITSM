package com.iitsm.user;

import com.iitsm.common.ApiResponse;
import com.iitsm.common.PagedResponse;
import com.iitsm.user.dto.AdminUserResponse;
import com.iitsm.user.dto.UserActiveRequest;
import com.iitsm.user.dto.UserRoleRequest;
import com.iitsm.user.service.UserAdministrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {
    private final UserAdministrationService service;
    public AdminUserController(UserAdministrationService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PagedResponse<AdminUserResponse>> search(@RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        var result = service.search(query, page, size);
        return ApiResponse.ok("Users retrieved", PagedResponse.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> get(@PathVariable Long id) { return ApiResponse.ok(service.get(id)); }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminUserResponse> setActive(@PathVariable Long id, @Valid @RequestBody UserActiveRequest request, Authentication auth) {
        return ApiResponse.ok("User account updated", service.setActive(id, request.active(), auth.getName()));
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<AdminUserResponse> setRole(@PathVariable Long id, @Valid @RequestBody UserRoleRequest request, Authentication auth) {
        return ApiResponse.ok("User role updated", service.setRole(id, request.role(), auth.getName()));
    }
}
