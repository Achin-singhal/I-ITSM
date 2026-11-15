package com.iitsm.assignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignmentSelectionRequest(@NotNull @Positive Long technicianId) { }
