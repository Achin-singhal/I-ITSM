package com.iitsm.servicecatalog.dto;

import jakarta.validation.constraints.NotNull;

public record ItServiceActiveRequest(@NotNull Boolean active) { }
