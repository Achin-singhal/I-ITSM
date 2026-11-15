package com.iitsm.auth;

import com.iitsm.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        if (expirationMs <= 0) throw new IllegalStateException("JWT_EXPIRATION_MS must be positive");
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String email, Role role) {
        Instant now = Instant.now();
        return Jwts.builder().subject(email).claim("uid", userId).claim("role", effectiveRole(role).name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key).compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getExpirationMs() { return expirationMs; }

    public static Role effectiveRole(Role role) {
        return role == Role.AGENT ? Role.TECHNICIAN : role;
    }
}
