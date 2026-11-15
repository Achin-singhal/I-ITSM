package com.iitsm.user.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Profile("!test")
public class AdminPasswordResetRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminPasswordResetRunner.class);

    private final boolean enabled;
    private final String email;
    private final AdminPasswordResetPrompt prompt;
    private final AdminPasswordResetService resetService;

    public AdminPasswordResetRunner(
            @Value("${admin.password-reset.enabled:${ADMIN_PASSWORD_RESET_ENABLED:false}}") boolean enabled,
            @Value("${admin.password-reset.email:${ADMIN_PASSWORD_RESET_EMAIL:}}") String email,
            AdminPasswordResetPrompt prompt,
            AdminPasswordResetService resetService) {
        this.enabled = enabled;
        this.email = email;
        this.prompt = prompt;
        this.resetService = resetService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Administrator password reset skipped because it is disabled.");
            return;
        }
        String targetEmail = email == null ? "" : email.trim();
        if (targetEmail.isBlank()) {
            throw new IllegalStateException("Administrator password reset email configuration is required.");
        }

        char[] password = null;
        char[] confirmation = null;
        try {
            password = prompt.readNewPassword();
            confirmation = prompt.readConfirmation();
            if (password == null || confirmation == null) {
                throw new IllegalStateException("Administrator password reset was cancelled.");
            }
            if (password.length < 8) {
                throw new IllegalArgumentException("Administrator password must contain at least 8 characters.");
            }
            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Password confirmation did not match.");
            }
            resetService.resetExistingActiveAdministrator(targetEmail, password);
            log.info("Password reset successfully for an existing active administrator.");
        } catch (RuntimeException ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            log.error("Administrator password reset failed. exceptionType={} rootCauseType={}",
                    ex.getClass().getName(), root.getClass().getName());
            throw new IllegalStateException("Administrator password reset failed. No credentials were logged.");
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
