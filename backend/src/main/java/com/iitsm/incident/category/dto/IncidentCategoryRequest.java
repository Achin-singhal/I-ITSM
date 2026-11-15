package com.iitsm.incident.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidentCategoryRequest(@NotBlank @Size(max = 64) String name) { }
