package com.iitsm.user.dto;

import com.iitsm.user.entity.User;
import java.time.Instant;

/** Safe administrative projection; authentication secrets are never serialized. */
public record AdminUserResponse(Long id, String email, String fullName, String role,
                                boolean active, Instant createdAt) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), user.isActive(), user.getCreatedAt());
    }
}
