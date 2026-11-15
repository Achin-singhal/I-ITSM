package com.iitsm.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsm.common.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, ObjectMapper objectMapper) { this.jwtService = jwtService; this.userRepository = userRepository; this.objectMapper = objectMapper; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parseAndValidate(header.substring(7));
                String role = claims.get("role", String.class);
                Long userId = claims.get("uid", Long.class);
                if (claims.getSubject() == null || role == null || userId == null) throw new JwtException("Required claims missing");
                User user = userRepository.findByEmail(claims.getSubject()).filter(User::isActive)
                        .filter(found -> found.getId().equals(userId) && JwtService.effectiveRole(found.getRole()).name().equals(role))
                        .orElseThrow(() -> new JwtException("Token identity is no longer active"));
                var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                objectMapper.writeValue(response.getOutputStream(), ApiResponse.error("Invalid or expired bearer token"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
