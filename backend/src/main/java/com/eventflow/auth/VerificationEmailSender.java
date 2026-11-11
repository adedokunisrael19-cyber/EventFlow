package com.eventflow.auth;

public interface VerificationEmailSender {

    void send(String email, String token);
}
