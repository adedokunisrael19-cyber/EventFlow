package com.eventflow.auth;

public record VerificationEmailRequested(String email, String token) {
}
