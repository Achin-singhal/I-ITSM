package com.iitsm.user.service;

import com.iitsm.user.entity.Role;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.audit.service.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service encapsulating User business logic.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService audit) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    public User createUser(String email, String rawPassword, String fullName, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email already exists: " + email);
        }
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(email, encodedPassword, fullName, role);
        User saved = userRepository.saveAndFlush(user);
        User actor = role == Role.EMPLOYEE ? saved : null;
        String description = role == Role.EMPLOYEE ? "Employee account registered" : "User account created through trusted provisioning";
        audit.recordEntityAction("USER", saved.getId(), actor, "USER_CREATED", null, role.name(), description);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<User> getActiveAgents() {
        return userRepository.findByRoleAndActiveTrue(Role.AGENT);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
