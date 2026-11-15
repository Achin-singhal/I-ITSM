package com.iitsm.user.bootstrap;

import com.iitsm.user.entity.Role;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AdminPasswordResetTest {
    private static final String EMAIL = "ADMIN@gmail.com";
    private static final String FULL_NAME = "Existing Administrator";
    private static final String OLD_PASSWORD = "old-password";
    private static final String NEW_PASSWORD = "new-secure-password";

    @Test
    void successfullyResetsOnlyExistingAdminsPasswordHash() {
        var encoder = new BCryptPasswordEncoder();
        var user = new User(EMAIL, encoder.encode(OLD_PASSWORD), FULL_NAME, Role.ADMIN);
        user.setId(42L);
        user.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2025-02-01T00:00:00Z"));
        var state = new UserState(user);
        char[] newPassword = NEW_PASSWORD.toCharArray();
        char[] confirmation = NEW_PASSWORD.toCharArray();
        var runner = runner(state, encoder, () -> newPassword, () -> confirmation);

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(encoder.matches(NEW_PASSWORD, user.getPasswordHash()));
        assertFalse(encoder.matches(OLD_PASSWORD, user.getPasswordHash()));
        assertEquals(EMAIL, user.getEmail());
        assertEquals(FULL_NAME, user.getFullName());
        assertEquals(Role.ADMIN, user.getRole());
        assertTrue(user.isActive());
        assertEquals(42L, user.getId());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), user.getCreatedAt());
        assertEquals(Instant.parse("2025-02-01T00:00:00Z"), user.getUpdatedAt());
        assertArrayEquals(new char[newPassword.length], newPassword);
        assertArrayEquals(new char[confirmation.length], confirmation);
        assertEquals(1, state.updates.get());
    }

    @Test
    void missingAccountIsRejectedWithoutUpdate() {
        var state = new UserState(null);
        assertThrows(IllegalStateException.class, () -> runner(state, new BCryptPasswordEncoder(),
                () -> NEW_PASSWORD.toCharArray(), () -> NEW_PASSWORD.toCharArray())
                .run(new DefaultApplicationArguments(new String[0])));
        assertEquals(0, state.updates.get());
    }

    @Test
    void inactiveAdministratorIsRejectedWithoutUpdate() {
        User user = admin();
        user.setActive(false);
        var state = new UserState(user);
        assertThrows(IllegalStateException.class, () -> runner(state, new BCryptPasswordEncoder(),
                () -> NEW_PASSWORD.toCharArray(), () -> NEW_PASSWORD.toCharArray())
                .run(new DefaultApplicationArguments(new String[0])));
        assertEquals(0, state.updates.get());
    }

    @Test
    void nonAdministratorIsRejectedWithoutUpdate() {
        User user = new User(EMAIL, "unchanged", FULL_NAME, Role.EMPLOYEE);
        var state = new UserState(user);
        assertThrows(IllegalStateException.class, () -> runner(state, new BCryptPasswordEncoder(),
                () -> NEW_PASSWORD.toCharArray(), () -> NEW_PASSWORD.toCharArray())
                .run(new DefaultApplicationArguments(new String[0])));
        assertEquals("unchanged", user.getPasswordHash());
        assertEquals(0, state.updates.get());
    }

    @Test
    void mismatchedPasswordConfirmationIsRejectedAndBuffersAreCleared() {
        User user = admin();
        var state = new UserState(user);
        char[] first = NEW_PASSWORD.toCharArray();
        char[] second = "different-password".toCharArray();
        String oldHash = user.getPasswordHash();

        assertThrows(IllegalStateException.class, () -> runner(state, new BCryptPasswordEncoder(),
                () -> first, () -> second).run(new DefaultApplicationArguments(new String[0])));

        assertEquals(oldHash, user.getPasswordHash());
        assertEquals(0, state.updates.get());
        assertArrayEquals(new char[first.length], first);
        assertArrayEquals(new char[second.length], second);
    }

    private static User admin() {
        return new User(EMAIL, new BCryptPasswordEncoder().encode(OLD_PASSWORD), FULL_NAME, Role.ADMIN);
    }

    private static AdminPasswordResetRunner runner(UserState state, BCryptPasswordEncoder encoder,
            java.util.function.Supplier<char[]> first, java.util.function.Supplier<char[]> second) {
        AdminPasswordResetPrompt prompt = new AdminPasswordResetPrompt() {
            @Override public char[] readNewPassword() { return first.get(); }
            @Override public char[] readConfirmation() { return second.get(); }
        };
        return new AdminPasswordResetRunner(true, EMAIL, prompt,
                new AdminPasswordResetService(state.repository(), encoder));
    }

    private static final class UserState {
        private final User user;
        private final AtomicInteger updates = new AtomicInteger();
        private final AtomicReference<String> updatedEmail = new AtomicReference<>();

        private UserState(User user) { this.user = user; }

        private UserRepository repository() {
            return (UserRepository) Proxy.newProxyInstance(UserRepository.class.getClassLoader(),
                    new Class<?>[]{UserRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "findByEmail" -> Optional.ofNullable(user);
                        case "updateActiveUserPasswordHash" -> {
                            String email = (String) args[0];
                            Role role = (Role) args[1];
                            String hash = (String) args[2];
                            if (user != null && user.getEmail().equals(email) && user.getRole() == role && user.isActive()) {
                                user.setPasswordHash(hash);
                                updatedEmail.set(email);
                                updates.incrementAndGet();
                                yield 1;
                            }
                            yield 0;
                        }
                        case "toString" -> "PasswordResetUserRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
