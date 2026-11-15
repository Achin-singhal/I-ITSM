package com.iitsm.user.bootstrap;

import org.springframework.stereotype.Component;

import java.io.Console;

@Component
public class ConsoleAdminPasswordResetPrompt implements AdminPasswordResetPrompt {
    @Override
    public char[] readNewPassword() {
        return requireConsole().readPassword("Enter the new administrator password (input is hidden; minimum 8 characters): ");
    }

    @Override
    public char[] readConfirmation() {
        return requireConsole().readPassword("Confirm the new administrator password (input is hidden): ");
    }

    private Console requireConsole() {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Administrator password reset requires an interactive backend terminal.");
        }
        return console;
    }
}
