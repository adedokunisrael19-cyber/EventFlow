package com.eventflow.auth;

public class PasswordAlreadySetException extends RuntimeException {

    public PasswordAlreadySetException() {
        super("A password is already configured for this account.");
    }
}
