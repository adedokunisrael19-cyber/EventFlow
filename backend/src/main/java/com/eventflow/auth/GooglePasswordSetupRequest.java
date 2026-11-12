package com.eventflow.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GooglePasswordSetupRequest(
        @NotBlank String idToken,
        @NotBlank @Size(min = 12, max = 72) String password
) {
}
