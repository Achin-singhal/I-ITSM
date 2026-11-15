package com.iitsm.incident.category.dto;

import jakarta.validation.constraints.NotNull;

public record IncidentCategoryActiveRequest(@NotNull Boolean active) { }
