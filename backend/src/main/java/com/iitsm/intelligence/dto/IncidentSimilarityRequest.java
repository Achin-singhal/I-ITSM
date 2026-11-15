package com.iitsm.intelligence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidentSimilarityRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String description,
        @NotBlank @Size(max = 64) String category,
        @NotBlank @Size(max = 120) String affectedService) { }
