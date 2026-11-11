package com.eventflow.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationRequest(
        @NotBlank
        @Size(max = 128)
        String token
) {
}
