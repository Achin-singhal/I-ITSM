package com.iitsm.user.bootstrap;

import com.iitsm.user.entity.Role;
import com.iitsm.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminPasswordResetService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminPasswordResetService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public void resetExistingActiveAdministrator(String email, char[] newPassword) {
        var user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account exists for the configured email."));
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("The target account is not an administrator.");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("The target administrator account is inactive.");
        }

        String encoded = passwordEncoder.encode(new String(newPassword));
        int updated = users.updateActiveUserPasswordHash(email, Role.ADMIN, encoded);
        if (updated != 1) {
            throw new IllegalStateException("The target account changed during password reset; no password was updated.");
        }
    }
}
