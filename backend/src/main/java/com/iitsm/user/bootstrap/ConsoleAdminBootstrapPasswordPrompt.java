package com.iitsm.user.bootstrap;

import org.springframework.stereotype.Component;

import java.io.Console;

@Component
public class ConsoleAdminBootstrapPasswordPrompt implements AdminBootstrapPasswordPrompt {
    @Override
    public char[] readPassword() {
        return requireConsole().readPassword(
                "Enter the first administrator password (input is hidden; minimum 6 characters): ");
    }

    @Override
    public void reportPasswordTooShort() {
        Console console = requireConsole();
        console.writer().println("Password must contain at least 6 characters. Please try again.");
        console.flush();
    }

    private Console requireConsole() {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("First-admin bootstrap requires an interactive backend terminal.");
        }
        return console;
    }
}
