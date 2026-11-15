package com.iitsm.report.dto;

public record TechnicianMetricsResponse(Long technicianId, String name, String email, boolean available,
        int maxActiveWorkload, long assignedIncidentCount, long activeWorkload,
        long resolvedIncidentCount, Double averageResolutionSeconds) { }
