package com.eventflow.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

@Component
public class GoogleIdTokenVerifierAdapter implements GoogleIdentityTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierAdapter(@Value("${eventflow.google.client-id:}") String clientId) {
        this.verifier = clientId.isBlank() ? null : new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    @Override
    public Optional<GoogleIdentityClaims> verify(String rawToken) {
        if (verifier == null || rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        try {
            GoogleIdToken token = verifier.verify(rawToken);
            if (token == null) return Optional.empty();
            GoogleIdToken.Payload payload = token.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            if (subject == null || subject.isBlank() || email == null || email.isBlank()
                    || name == null || name.isBlank() || !emailVerified) {
                return Optional.empty();
            }
            return Optional.of(new GoogleIdentityClaims(subject, email, name, true));
        } catch (GeneralSecurityException | IOException exception) {
            return Optional.empty();
        }
    }
}
