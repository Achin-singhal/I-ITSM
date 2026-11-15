package com.iitsm.incident.category.dto;

import com.iitsm.incident.category.entity.IncidentCategory;

public record IncidentCategoryResponse(Long id, String name, boolean active) {
    public static IncidentCategoryResponse from(IncidentCategory category) {
        return new IncidentCategoryResponse(category.getId(), category.getName(), category.isActive());
    }
}
