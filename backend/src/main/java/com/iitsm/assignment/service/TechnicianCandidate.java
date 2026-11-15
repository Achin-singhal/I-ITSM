package com.iitsm.assignment.service;

import com.iitsm.assignment.entity.TechnicianProfile;
public record TechnicianCandidate(TechnicianProfile profile, long activeWorkload) {
    public boolean eligible() { return profile.isAvailable() && activeWorkload < profile.getMaxActiveWorkload() && profile.getUser().isActive(); }
}
