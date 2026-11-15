package com.iitsm.auth;

import com.iitsm.auth.dto.AuthResponse;
import com.iitsm.auth.dto.LoginRequest;
import com.iitsm.auth.dto.RegisterRequest;
import com.iitsm.user.entity.Role;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * Handles authentication and registration.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()) || !user.isActive())
            throw new IllegalArgumentException("Invalid email or password");

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), JwtService.effectiveRole(user.getRole()), jwtService.getExpirationMs());
    }

    public AuthResponse register(RegisterRequest request) {
        User user = userService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getFullName(),
                Role.EMPLOYEE
        );
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), Role.EMPLOYEE, jwtService.getExpirationMs());
    }
}
