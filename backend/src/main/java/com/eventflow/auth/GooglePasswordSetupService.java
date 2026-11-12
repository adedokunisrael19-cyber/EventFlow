package com.eventflow.auth;

import com.eventflow.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GooglePasswordSetupService {

    private static final String PROVIDER = "GOOGLE";

    private final GoogleIdentityTokenVerifier tokenVerifier;
    private final ExternalIdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;

    public GooglePasswordSetupService(
            GoogleIdentityTokenVerifier tokenVerifier,
            ExternalIdentityRepository identityRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.tokenVerifier = tokenVerifier;
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void setPassword(UUID currentUserId, String googleIdToken, String rawPassword) {
        GoogleIdentityTokenVerifier.GoogleIdentityClaims claims = tokenVerifier.verify(googleIdToken)
                .filter(GoogleIdentityTokenVerifier.GoogleIdentityClaims::emailVerified)
                .orElseThrow(InvalidCredentialsException::new);

        User user = identityRepository.findByProviderAndProviderSubject(PROVIDER, claims.subject())
                .map(ExternalIdentity::getUser)
                .filter(identityUser -> identityUser.getId().equals(currentUserId))
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPasswordHash() != null) {
            throw new PasswordAlreadySetException();
        }

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
    }
}
