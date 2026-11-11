package com.eventflow.auth;

import java.time.Instant;

public record AuthenticationResponse(String accessToken, String tokenType, Instant expiresAt) {
}
