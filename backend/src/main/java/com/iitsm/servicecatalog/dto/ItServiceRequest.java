package com.iitsm.servicecatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItServiceRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description) { }
