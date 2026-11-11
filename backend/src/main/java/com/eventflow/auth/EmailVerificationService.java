package com.eventflow.auth;

import com.eventflow.user.User;
import com.eventflow.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class EmailVerificationService {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void issueFor(User user) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant now = clock.instant();

        tokenRepository.save(new EmailVerificationToken(user, TokenHasher.sha256(rawToken), now.plus(TOKEN_LIFETIME)));
        eventPublisher.publishEvent(new VerificationEmailRequested(user.getEmail(), rawToken));
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHashForUpdate(TokenHasher.sha256(rawToken))
                .orElseThrow(InvalidEmailVerificationTokenException::new);
        Instant now = clock.instant();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new InvalidEmailVerificationTokenException();
        }

        token.markUsed(now);
        User user = token.getUser();
        user.markEmailVerified();
        userRepository.save(user);
        tokenRepository.save(token);
    }

}
