package com.eventflow.auth;

import com.eventflow.user.User;
import com.eventflow.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class AuthenticationService {

    private static final Duration SESSION_LIFETIME = Duration.ofHours(8);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthenticationService(
            UserRepository userRepository,
            UserSessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AuthenticationResponse login(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash())
                || !user.isEmailVerified()) {
            throw new InvalidCredentialsException();
        }

        return createSession(user);
    }

    @Transactional
    public AuthenticationResponse createSession(User user) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expiresAt = clock.instant().plus(SESSION_LIFETIME);
        sessionRepository.save(new UserSession(user, TokenHasher.sha256(rawToken), expiresAt));

        return new AuthenticationResponse(rawToken, "Bearer", expiresAt);
    }

    @Transactional
    public void logout(java.util.UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> session.revoke(clock.instant()));
    }
}
