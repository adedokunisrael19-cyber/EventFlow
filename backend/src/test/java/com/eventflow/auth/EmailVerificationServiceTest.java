package com.eventflow.auth;

import com.eventflow.user.User;
import com.eventflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    private EmailVerificationTokenRepository tokenRepository;
    private UserRepository userRepository;
    private ApplicationEventPublisher eventPublisher;
    private EmailVerificationService service;
    private final Instant now = Instant.parse("2026-11-11T12:00:00Z");

    @BeforeEach
    void setUp() {
        tokenRepository = mock(EmailVerificationTokenRepository.class);
        userRepository = mock(UserRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new EmailVerificationService(
                tokenRepository,
                userRepository,
                eventPublisher,
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    @Test
    void issuesAnExpiringHashedTokenAndPublishesAnEmailRequest() {
        User user = new User("owner@example.com", "Ada Planner", "encoded-password");
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.issueFor(user);

        var tokenCaptor = org.mockito.ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        EmailVerificationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getTokenHash()).hasSize(64).doesNotContain("=");
        assertThat(savedToken.getExpiresAt()).isEqualTo(now.plusSeconds(86_400));

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(VerificationEmailRequested.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().email()).isEqualTo("owner@example.com");
        assertThat(eventCaptor.getValue().token()).isNotEqualTo(savedToken.getTokenHash());
        assertThat(eventCaptor.getValue().token()).hasSize(43);
    }

    @Test
    void verifiesTheUserAndConsumesTheToken() {
        User user = new User("owner@example.com", "Ada Planner", "encoded-password");
        EmailVerificationToken token = new EmailVerificationToken(user, "digest", now.plusSeconds(60));
        when(tokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(token));

        service.verify("raw-token-value-that-is-long-enough");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(now);
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);

        assertThatThrownBy(() -> service.verify("raw-token-value-that-is-long-enough"))
                .isInstanceOf(InvalidEmailVerificationTokenException.class);
    }

    @Test
    void rejectsExpiredTokens() {
        User user = new User("owner@example.com", "Ada Planner", "encoded-password");
        EmailVerificationToken token = new EmailVerificationToken(user, "digest", now);
        when(tokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verify("raw-token-value-that-is-long-enough"))
                .isInstanceOf(InvalidEmailVerificationTokenException.class);

        assertThat(user.isEmailVerified()).isFalse();
    }
}
