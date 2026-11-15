package com.iitsm.auth;

import com.iitsm.user.entity.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes-long";

    @Test void createsSignedJwtWithIdentityAndRoleClaims() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generateToken(7L, "employee@example.test", Role.EMPLOYEE);
        assertEquals("employee@example.test", service.parseAndValidate(token).getSubject());
        assertEquals(7, service.parseAndValidate(token).get("uid", Integer.class));
        assertEquals("EMPLOYEE", service.parseAndValidate(token).get("role", String.class));
        assertEquals(3, token.split("\\.").length);
    }

    @Test void rejectsTokenSignedByDifferentKey() {
        String token = new JwtService(SECRET, 60_000).generateToken(7L, "employee@example.test", Role.EMPLOYEE);
        assertThrows(JwtException.class, () -> new JwtService("another-test-secret-with-32-bytes-minimum", 60_000).parseAndValidate(token));
    }

    @Test void rejectsWeakSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60_000));
    }

    @Test void mapsLegacyAgentRoleToTechnicianAuthority() {
        assertEquals(Role.TECHNICIAN, JwtService.effectiveRole(Role.AGENT));
    }
}
