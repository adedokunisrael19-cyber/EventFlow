package com.eventflow.auth;

import java.security.Principal;
import java.util.UUID;

public record CurrentUserPrincipal(
        UUID userId,
        UUID sessionId,
        String email,
        String fullName,
        UUID workspaceId,
        String workspaceName,
        String role,
        boolean hasPassword
) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
