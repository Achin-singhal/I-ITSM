package com.iitsm.user.bootstrap;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.iitsm.user.entity.Role;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.user.service.UserService;
import com.iitsm.audit.service.AuditService;
import com.iitsm.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AdminBootstrapTest {
    private static final String EMAIL = "first.admin@example.test";
    private static final String FULL_NAME = "First Administrator";
    private static final String RAW_PASSWORD = "secure-local-password";

    @Test
    void disabledBootstrapDoesNotCheckDatabaseOrPromptForPassword() {
        var state = new FakeUsers();
        AtomicInteger promptCalls = new AtomicInteger();
        var runner = runner(false, EMAIL, FULL_NAME, () -> {
            promptCalls.incrementAndGet();
            return RAW_PASSWORD.toCharArray();
        }, state);

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(0, state.roleChecks.get());
        assertEquals(0, promptCalls.get());
        assertNull(state.savedUser.get());
    }

    @Test
    void documentedEnvironmentVariablesBindToBootstrapProperties() {
        var prompt = new TestPrompt();
        var bootstrapService = new CapturingBootstrapService();
        new ApplicationContextRunner()
                .withUserConfiguration(PropertyBindingConfiguration.class)
                .withBean(AdminBootstrapPasswordPrompt.class, () -> prompt)
                .withBean(FirstAdminBootstrapService.class, () -> bootstrapService)
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("bootstrapTestEnvironment", Map.of(
                                "BOOTSTRAP_ADMIN_ENABLED", "true",
                                "BOOTSTRAP_ADMIN_EMAIL", EMAIL,
                                "BOOTSTRAP_ADMIN_FULL_NAME", FULL_NAME))))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    context.getBean(AdminBootstrapRunner.class).run(new DefaultApplicationArguments(new String[0]));
                    assertEquals(1, prompt.calls);
                    assertEquals(EMAIL, bootstrapService.email);
                    assertEquals(FULL_NAME, bootstrapService.fullName);
                    assertEquals(RAW_PASSWORD, bootstrapService.password);
                });
    }

    @Test
    void runnerIsExcludedFromTestProfileEvenWhenBootstrapIsEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestProfileConfiguration.class)
                .withPropertyValues("spring.profiles.active=test", "BOOTSTRAP_ADMIN_ENABLED=true")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(0, context.getBeanNamesForType(AdminBootstrapRunner.class).length);
                });
    }

    @Test
    void enabledBootstrapCreatesExactlyOneActiveAdministratorAndClearsPromptBuffer() {
        var state = new FakeUsers();
        char[] password = RAW_PASSWORD.toCharArray();
        var runner = runner(true, EMAIL, FULL_NAME, () -> password, state);

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(2, state.roleChecks.get());
        assertEquals(1, state.emailChecks.get());
        assertEquals(1, state.saves.get());
        assertEquals(Role.ADMIN, state.savedUser.get().getRole());
        assertTrue(state.savedUser.get().isActive());
        assertArrayEquals(new char[password.length], password);
    }

    @Test
    void shortPasswordIsClearedAndPromptRetriesUntilPasswordIsLongEnough() {
        var state = new FakeUsers();
        char[] rejected = "abc".toCharArray();
        char[] accepted = RAW_PASSWORD.toCharArray();
        AtomicInteger promptCalls = new AtomicInteger();
        AtomicInteger validationMessages = new AtomicInteger();
        AdminBootstrapPasswordPrompt prompt = new AdminBootstrapPasswordPrompt() {
            @Override public char[] readPassword() {
                return promptCalls.getAndIncrement() == 0 ? rejected : accepted;
            }
            @Override public void reportPasswordTooShort() { validationMessages.incrementAndGet(); }
        };
        var runner = runner(true, EMAIL, FULL_NAME, prompt, state);

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(2, promptCalls.get());
        assertEquals(1, validationMessages.get());
        assertArrayEquals(new char[rejected.length], rejected);
        assertArrayEquals(new char[accepted.length], accepted);
        assertEquals(1, state.saves.get());
        assertTrue(new BCryptPasswordEncoder().matches(RAW_PASSWORD, state.savedUser.get().getPasswordHash()));
    }

    @Test
    void existingAdministratorPreventsPromptAndCreation() {
        var state = new FakeUsers();
        state.adminExists.set(true);
        AtomicInteger promptCalls = new AtomicInteger();
        var runner = runner(true, EMAIL, FULL_NAME, () -> {
            promptCalls.incrementAndGet();
            return RAW_PASSWORD.toCharArray();
        }, state);

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(1, state.roleChecks.get());
        assertEquals(0, promptCalls.get());
        assertEquals(0, state.saves.get());
    }

    @Test
    void transactionalBootstrapRefusesWhenAnAdministratorExists() {
        var state = new FakeUsers();
        state.adminExistsAfterChecks.set(true);
        var bootstrap = new FirstAdminBootstrapService(state.repository(), new UserService(state.repository(), new BCryptPasswordEncoder(), auditService()));

        assertFalse(bootstrap.createFirstAdministrator(EMAIL, FULL_NAME, RAW_PASSWORD));

        assertEquals(1, state.roleChecks.get());
        assertEquals(0, state.saves.get());
    }

    @Test
    void passwordIsEncodedByTheExistingEncoderBeforePersistence() {
        var state = new FakeUsers();
        var encoder = new BCryptPasswordEncoder();
        var bootstrap = new FirstAdminBootstrapService(state.repository(), new UserService(state.repository(), encoder, auditService()));

        assertTrue(bootstrap.createFirstAdministrator(EMAIL, FULL_NAME, RAW_PASSWORD));

        String persistedHash = state.savedUser.get().getPasswordHash();
        assertNotEquals(RAW_PASSWORD, persistedHash);
        assertTrue(encoder.matches(RAW_PASSWORD, persistedHash));
        assertEquals(1, state.saves.get());
    }

    @Test
    void bootstrapErrorsDoNotExposePasswordOrPersistenceDetails() {
        String sensitivePassword = RAW_PASSWORD;
        String sensitiveHash = "private-hash-value";
        char[] password = sensitivePassword.toCharArray();
        FirstAdminBootstrapService failingService = new FirstAdminBootstrapService(null, null) {
            @Override public boolean administratorExists() { return false; }
            @Override public boolean createFirstAdministrator(String email, String fullName, String rawPassword) {
                throw new IllegalStateException("password=" + sensitivePassword,
                        new IllegalArgumentException("hash=" + sensitiveHash));
            }
        };
        var runner = new AdminBootstrapRunner(true, EMAIL, FULL_NAME, () -> password, failingService);
        Logger logger = (Logger) LoggerFactory.getLogger(AdminBootstrapRunner.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> runner.run(new DefaultApplicationArguments(new String[0])));

            String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertTrue(error.getMessage().contains("No credentials were logged"));
            assertFalse(error.getMessage().contains(sensitivePassword));
            assertTrue(logged.contains("exceptionType=java.lang.IllegalStateException"));
            assertTrue(logged.contains("rootCauseType=java.lang.IllegalArgumentException"));
            assertFalse(logged.contains(sensitivePassword));
            assertFalse(logged.contains(sensitiveHash));
            assertTrue(appender.list.stream().allMatch(event -> event.getThrowableProxy() == null));
            assertArrayEquals(new char[password.length], password);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static AdminBootstrapRunner runner(boolean enabled, String email, String fullName,
            AdminBootstrapPasswordPrompt prompt, FakeUsers state) {
        UserRepository repository = state.repository();
        var service = new FirstAdminBootstrapService(repository, new UserService(repository, new BCryptPasswordEncoder(), auditService()));
        return new AdminBootstrapRunner(enabled, email, fullName, prompt, service);
    }

    private static AuditService auditService() {
        AuditLogRepository repository = (AuditLogRepository) Proxy.newProxyInstance(
                AuditLogRepository.class.getClassLoader(), new Class<?>[]{AuditLogRepository.class},
                (proxy, method, args) -> "save".equals(method.getName()) ? args[0] : null);
        return new AuditService(repository);
    }

    private static final class FakeUsers {
        private final AtomicBoolean adminExists = new AtomicBoolean();
        private final AtomicBoolean adminExistsAfterChecks = new AtomicBoolean();
        private final AtomicInteger roleChecks = new AtomicInteger();
        private final AtomicInteger emailChecks = new AtomicInteger();
        private final AtomicInteger saves = new AtomicInteger();
        private final AtomicReference<User> savedUser = new AtomicReference<>();

        private UserRepository repository() {
            return (UserRepository) Proxy.newProxyInstance(UserRepository.class.getClassLoader(),
                    new Class<?>[]{UserRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "existsByRole" -> {
                            roleChecks.incrementAndGet();
                            yield adminExists.get() || adminExistsAfterChecks.get();
                        }
                        case "existsByEmail" -> {
                            emailChecks.incrementAndGet();
                            yield false;
                        }
                        case "save", "saveAndFlush" -> {
                            saves.incrementAndGet();
                            User user = (User) args[0];
                            savedUser.set(user);
                            yield user;
                        }
                        case "toString" -> "FakeUserRepository";
                        default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                    });
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AdminBootstrapRunner.class)
    static class PropertyBindingConfiguration { }

    @Configuration(proxyBeanMethods = false)
    @Import(AdminBootstrapRunner.class)
    static class TestProfileConfiguration { }

    private static final class TestPrompt implements AdminBootstrapPasswordPrompt {
        private int calls;
        @Override public char[] readPassword() { calls++; return RAW_PASSWORD.toCharArray(); }
    }

    private static final class CapturingBootstrapService extends FirstAdminBootstrapService {
        private String email;
        private String fullName;
        private String password;

        private CapturingBootstrapService() { super(null, null); }
        @Override public boolean administratorExists() { return false; }
        @Override public boolean createFirstAdministrator(String email, String fullName, String rawPassword) {
            this.email = email;
            this.fullName = fullName;
            this.password = rawPassword;
            return true;
        }
    }
}
