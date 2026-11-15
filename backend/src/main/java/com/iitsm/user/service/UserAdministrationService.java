package com.iitsm.user.service;

import com.iitsm.user.dto.AdminUserResponse;
import com.iitsm.user.entity.Role;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserAdministrationService {
    private final UserRepository users;
    private final AuditService audit;

    public UserAdministrationService(UserRepository users, AuditService audit) { this.users = users; this.audit = audit; }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> search(String query, int page, int size) {
        return users.searchUsers(normalize(query), PageRequest.of(page, size)).map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(Long id) { return AdminUserResponse.from(user(id)); }

    public AdminUserResponse setActive(Long id, boolean active, String actorEmail) {
        users.lockActiveAdministrators(Role.ADMIN);
        var target = user(id);
        if (target.getEmail().equalsIgnoreCase(actorEmail))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Administrators cannot deactivate their own account");
        if (!active && target.getRole() == Role.ADMIN && target.isActive()
                && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active administrator must remain");
        boolean previous = target.isActive();
        if (previous != active) {
            target.setActive(active);
            users.save(target);
            audit.recordEntityAction("USER", target.getId(), actor(actorEmail), active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                    Boolean.toString(previous), Boolean.toString(active), active ? "User account activated" : "User account deactivated");
        }
        return AdminUserResponse.from(target);
    }

    public AdminUserResponse setRole(Long id, String roleName, String actorEmail) {
        users.lockActiveAdministrators(Role.ADMIN);
        var target = user(id);
        if (target.getEmail().equalsIgnoreCase(actorEmail))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Administrators cannot change their own role");
        Role role;
        try { role = Role.valueOf(roleName); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role"); }
        if (!java.util.Set.of(Role.EMPLOYEE, Role.TECHNICIAN, Role.ADMIN).contains(role))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
        if (target.isActive() && target.getRole() == Role.ADMIN && role != Role.ADMIN
                && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active administrator must remain");
        Role previous = target.getRole();
        if (previous != role) {
            target.setRole(role);
            users.save(target);
            audit.recordEntityAction("USER", target.getId(), actor(actorEmail), "USER_ROLE_CHANGED",
                    previous.name(), role.name(), "User role changed");
        }
        return AdminUserResponse.from(target);
    }

    private com.iitsm.user.entity.User user(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
    private com.iitsm.user.entity.User actor(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
    private String normalize(String query) { return query == null || query.isBlank() ? null : query.trim(); }
}
