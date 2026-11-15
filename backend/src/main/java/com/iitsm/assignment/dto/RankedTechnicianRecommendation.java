package com.iitsm.assignment.dto;

import java.util.List;

public record RankedTechnicianRecommendation(Long technicianId, String name, String email,
        long activeWorkload, int maxActiveWorkload, int experienceYears, String strategy,
        int rank, List<String> reasons) { }
