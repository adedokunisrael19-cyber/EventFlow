package com.eventflow.auth;

import com.eventflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GooglePasswordSetupServiceTest {

    private GoogleIdentityTokenVerifier tokenVerifier;
    private ExternalIdentityRepository identityRepository;
    private PasswordEncoder passwordEncoder;
    private GooglePasswordSetupService service;

    @BeforeEach
    void setUp() {
        tokenVerifier = mock(GoogleIdentityTokenVerifier.class);
        identityRepository = mock(ExternalIdentityRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new GooglePasswordSetupService(tokenVerifier, identityRepository, passwordEncoder);
    }

    @Test
    void setsAnEncodedPasswordWhenGoogleIdentityMatchesCurrentUser() {
        User user = new User("owner@example.com", "Ada Planner", null);
        String googleToken = "fresh-google-id-token";
        when(tokenVerifier.verify(googleToken)).thenReturn(Optional.of(claims("google-sub-123")));
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.of(new ExternalIdentity(user, "GOOGLE", "google-sub-123")));
        when(passwordEncoder.encode("a-long-strong-password")).thenReturn("{bcrypt}encoded-password");

        service.setPassword(user.getId(), googleToken, "a-long-strong-password");

        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}encoded-password");
        verify(passwordEncoder).encode("a-long-strong-password");
    }

    @Test
    void rejectsGoogleIdentityThatBelongsToAnotherEventFlowUser() {
        User signedInUser = new User("owner@example.com", "Ada Planner", null);
        User otherUser = new User("other@example.com", "Other Planner", null);
        when(tokenVerifier.verify("fresh-google-id-token")).thenReturn(Optional.of(claims("other-google-sub")));
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "other-google-sub"))
                .thenReturn(Optional.of(new ExternalIdentity(otherUser, "GOOGLE", "other-google-sub")));

        assertThatThrownBy(() -> service.setPassword(signedInUser.getId(), "fresh-google-id-token", "a-long-strong-password"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(signedInUser.getPasswordHash()).isNull();
    }

    @Test
    void rejectsPasswordSetupWhenPasswordWasAlreadyConfigured() {
        User user = new User("owner@example.com", "Ada Planner", "{bcrypt}existing-password");
        when(tokenVerifier.verify("fresh-google-id-token")).thenReturn(Optional.of(claims("google-sub-123")));
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.of(new ExternalIdentity(user, "GOOGLE", "google-sub-123")));

        assertThatThrownBy(() -> service.setPassword(user.getId(), "fresh-google-id-token", "a-long-strong-password"))
                .isInstanceOf(PasswordAlreadySetException.class);
    }

    private GoogleIdentityTokenVerifier.GoogleIdentityClaims claims(String subject) {
        return new GoogleIdentityTokenVerifier.GoogleIdentityClaims(subject, "owner@example.com", "Ada Planner", true);
    }
}
