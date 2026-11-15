package com.iitsm.report.dto;

public record AnalyticsSummaryResponse(long totalIncidents, long openIncidents, long inProgressIncidents,
        long resolvedIncidents, long closedIncidents, long criticalHighIncidents, long slaWarnings,
        long slaBreaches, Double slaCompliancePercentage, Double averageResolutionSeconds) { }
