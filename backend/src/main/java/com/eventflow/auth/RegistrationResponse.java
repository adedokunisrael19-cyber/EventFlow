package com.eventflow.auth;

import java.util.UUID;

public record RegistrationResponse(
        UUID userId,
        UUID workspaceId,
        String email,
        String fullName,
        String businessName,
        String role,
        boolean emailVerificationRequired
) {
}
