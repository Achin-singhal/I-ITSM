package com.iitsm.user.bootstrap;

/** Reads a bootstrap password without echoing it to the terminal. */
@FunctionalInterface
public interface AdminBootstrapPasswordPrompt {
    char[] readPassword();

    default void reportPasswordTooShort() {
    }
}
