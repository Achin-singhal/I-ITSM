package com.iitsm.servicecatalog.dto;

import com.iitsm.servicecatalog.entity.ItService;

import java.time.Instant;

public record ItServiceResponse(Long id, String name, String description, boolean active,
                                Instant createdAt, Instant updatedAt) {
    public static ItServiceResponse from(ItService service) {
        return new ItServiceResponse(service.getId(), service.getName(), service.getDescription(),
                service.isActive(), service.getCreatedAt(), service.getUpdatedAt());
    }
}
