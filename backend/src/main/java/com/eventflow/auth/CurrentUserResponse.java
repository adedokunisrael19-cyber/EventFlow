package com.eventflow.auth;

import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String email,
        String fullName,
        UUID workspaceId,
        String workspaceName,
        String role,
        boolean hasPassword
) {

    static CurrentUserResponse from(CurrentUserPrincipal principal) {
        return new CurrentUserResponse(
                principal.userId(),
                principal.email(),
                principal.fullName(),
                principal.workspaceId(),
                principal.workspaceName(),
                principal.role(),
                principal.hasPassword()
        );
    }
}
