package com.iitsm.incident.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidentFeedbackRequest(@NotNull @Min(1) @Max(5) Integer rating,
                                      @Size(max = 2000) String comments) { }
