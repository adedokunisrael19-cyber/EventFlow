package com.eventflow.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByTokenHash(String tokenHash);
}
