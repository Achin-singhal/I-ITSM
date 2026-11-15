package com.iitsm.user.entity;

/**
 * System user roles governing authorization.
 */
public enum Role {
    ADMIN,
    TECHNICIAN,
    // Retained so existing database rows remain readable during role migration.
    MANAGER,
    AGENT,
    EMPLOYEE
}
