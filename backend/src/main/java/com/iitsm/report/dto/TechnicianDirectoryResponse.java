package com.iitsm.report.dto;

public record TechnicianDirectoryResponse(Long userId, String name, String email, boolean available,
        int maxActiveWorkload, long activeWorkload) { }
