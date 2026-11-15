package com.iitsm.user.bootstrap;

/** Reads both password entries without echoing them to the terminal. */
public interface AdminPasswordResetPrompt {
    char[] readNewPassword();
    char[] readConfirmation();
}
