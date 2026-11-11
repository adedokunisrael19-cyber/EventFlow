package com.eventflow.auth;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email and password could not be verified. Check your details and verify your email address.");
    }
}
