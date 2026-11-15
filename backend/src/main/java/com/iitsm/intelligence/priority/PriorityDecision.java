package com.iitsm.intelligence.priority;

import com.iitsm.incident.entity.IncidentPriority;

public record PriorityDecision(double score, IncidentPriority priority, String explanation) {}
