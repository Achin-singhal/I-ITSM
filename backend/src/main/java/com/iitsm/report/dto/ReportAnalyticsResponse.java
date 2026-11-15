package com.iitsm.report.dto;

import java.util.List;
import java.util.Map;

public record ReportAnalyticsResponse(AnalyticsSummaryResponse summary, Map<String,Long> incidentsByStatus,
        Map<String,Long> incidentsByPriority, Map<String,Long> incidentsByCategory,
        List<TrendPoint> createdOverTime, long resolvedIncidents, long unresolvedIncidents,
        long totalSlaTargets, long slaBreaches) { }
