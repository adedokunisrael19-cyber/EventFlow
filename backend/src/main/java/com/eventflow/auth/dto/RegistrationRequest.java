package com.eventflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 12, max = 72)
        String password,

        @NotBlank
        @Size(max = 160)
        String fullName,

        @NotBlank
        @Size(max = 160)
        String businessName
) {
}
