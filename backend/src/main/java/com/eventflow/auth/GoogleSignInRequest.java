package com.eventflow.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleSignInRequest(
        @NotBlank @Size(max = 8192) String idToken,
        @Size(max = 160) String businessName
) { }
