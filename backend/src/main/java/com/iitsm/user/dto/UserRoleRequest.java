package com.iitsm.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRoleRequest(@NotBlank @Pattern(regexp = "EMPLOYEE|TECHNICIAN|ADMIN") String role) { }
