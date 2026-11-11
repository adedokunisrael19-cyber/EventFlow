package com.eventflow.auth;

public class InvalidEmailVerificationTokenException extends RuntimeException {

    public InvalidEmailVerificationTokenException() {
        super("The verification link is invalid or has expired.");
    }
}
