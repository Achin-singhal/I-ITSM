package com.iitsm.user.bootstrap;

import com.iitsm.user.entity.Role;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FirstAdminBootstrapService {
    private final UserRepository users;
    private final UserService userService;

    public FirstAdminBootstrapService(UserRepository users, UserService userService) {
        this.users = users;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public boolean administratorExists() {
        return users.existsByRole(Role.ADMIN);
    }

    /** Returns false if another administrator was provisioned before this transaction began. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean createFirstAdministrator(String email, String fullName, String rawPassword) {
        if (users.existsByRole(Role.ADMIN)) {
            return false;
        }
        userService.createUser(email, rawPassword, fullName, Role.ADMIN);
        return true;
    }
}
