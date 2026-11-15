package com.iitsm.intelligence.dto;

import com.iitsm.incident.entity.IncidentStatus;

import java.util.List;

public record IncidentSimilarityMatch(Long incidentId, String ticketNumber, String title, IncidentStatus status,
                                      double similarity, List<String> reasons) { }
