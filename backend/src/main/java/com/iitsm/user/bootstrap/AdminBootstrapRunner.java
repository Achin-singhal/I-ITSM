package com.iitsm.user.bootstrap;

import com.iitsm.user.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;

@Component
@Profile("!test")
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final boolean enabled;
    private final String email;
    private final String fullName;
    private final AdminBootstrapPasswordPrompt passwordPrompt;
    private final FirstAdminBootstrapService bootstrapService;

    public AdminBootstrapRunner(
            @Value("${bootstrap.admin.enabled:${BOOTSTRAP_ADMIN_ENABLED:false}}") boolean enabled,
            @Value("${bootstrap.admin.email:${BOOTSTRAP_ADMIN_EMAIL:}}") String email,
            @Value("${bootstrap.admin.full-name:${BOOTSTRAP_ADMIN_FULL_NAME:}}") String fullName,
            AdminBootstrapPasswordPrompt passwordPrompt,
            FirstAdminBootstrapService bootstrapService) {
        this.enabled = enabled;
        this.email = email;
        this.fullName = fullName;
        this.passwordPrompt = passwordPrompt;
        this.bootstrapService = bootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("First-admin bootstrap: bootstrap.admin.enabled resolved to {}.", enabled);
        if (!enabled) {
            log.info("First-admin bootstrap skipped because it is disabled.");
            return;
        }
        log.info("First-admin bootstrap is starting.");
        log.info("Checking for an existing ADMIN account before bootstrap.");
        if (bootstrapService.administratorExists()) {
            log.warn("First-admin bootstrap refused: an ADMIN account already exists.");
            return;
        }

        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedName = fullName == null ? "" : fullName.trim();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches() || normalizedEmail.length() > 180
                || normalizedName.length() < 2 || normalizedName.length() > 120) {
            throw new IllegalStateException("First-admin bootstrap configuration is invalid; check BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_FULL_NAME.");
        }

        char[] password = passwordPrompt.readPassword();
        if (password == null) {
            throw new IllegalStateException("First-admin bootstrap was cancelled; no account was created.");
        }
        while (password.length < 6) {
            Arrays.fill(password, '\0');
            passwordPrompt.reportPasswordTooShort();
            password = passwordPrompt.readPassword();
            if (password == null) {
                throw new IllegalStateException("First-admin bootstrap was cancelled; no account was created.");
            }
        }
        try {
            boolean created = bootstrapService.createFirstAdministrator(normalizedEmail, normalizedName, new String(password));
            if (created) {
                log.info("First active ADMIN account created successfully.");
            } else {
                log.warn("First-admin bootstrap refused: an ADMIN account already exists.");
            }
        } catch (RuntimeException ex) {
            Throwable rootCause = rootCauseOf(ex);
            // Log only exception types: messages and SQL details can contain sensitive values.
            log.error("First-admin bootstrap failed. exceptionType={} rootCauseType={}",
                    ex.getClass().getName(), rootCause.getClass().getName());
            throw new IllegalStateException("First-admin bootstrap failed. No credentials were logged.");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static Throwable rootCauseOf(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }
}
