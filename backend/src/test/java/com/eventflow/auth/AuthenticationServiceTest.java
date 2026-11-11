package com.eventflow.auth;

import com.eventflow.user.User;
import com.eventflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private UserRepository userRepository;
    private UserSessionRepository sessionRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationService service;
    private final Instant now = Instant.parse("2026-11-11T12:00:00Z");

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionRepository = mock(UserSessionRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AuthenticationService(
                userRepository,
                sessionRepository,
                passwordEncoder,
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    @Test
    void verifiedUserGetsAnOpaqueHashedEightHourSession() {
        User user = verifiedUser();
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", user.getPasswordHash())).thenReturn(true);
        when(sessionRepository.save(any(UserSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticationResponse response = service.login(" Owner@Example.com ", "correct-password");

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).hasSize(43);
        assertThat(response.expiresAt()).isEqualTo(now.plusSeconds(28_800));

        var sessionCaptor = org.mockito.ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getTokenHash()).hasSize(64)
                .isNotEqualTo(response.accessToken());
        assertThat(sessionCaptor.getValue().getExpiresAt()).isEqualTo(response.expiresAt());
        assertThat(sessionCaptor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void rejectsInvalidPasswordWithoutCreatingASession() {
        User user = verifiedUser();
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.login("owner@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(sessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void rejectsUnverifiedEmailWithoutCreatingASession() {
        User user = new User("owner@example.com", "Ada Planner", "{bcrypt}encoded-password");
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> service.login("owner@example.com", "correct-password"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(sessionRepository, never()).save(any(UserSession.class));
    }

    private User verifiedUser() {
        User user = new User("owner@example.com", "Ada Planner", "{bcrypt}encoded-password");
        user.markEmailVerified();
        return user;
    }
}
