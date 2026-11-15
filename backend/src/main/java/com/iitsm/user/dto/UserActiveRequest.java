package com.iitsm.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserActiveRequest(@NotNull Boolean active) { }
