package com.eventflow.auth;

import java.util.Optional;

public interface GoogleIdentityTokenVerifier {
    Optional<GoogleIdentityClaims> verify(String idToken);

    record GoogleIdentityClaims(String subject, String email, String fullName, boolean emailVerified) { }
}
