package com.example.EDIP.Auth.repository;

import com.example.EDIP.Auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    // Find active session by user ID
    Optional<UserSession> findByUserIdAndIsActiveTrue(UUID userId);

    // Find session by hashed Access Token
    Optional<UserSession> findByTokenHash(String tokenHash);

    // Find session by hashed Refresh Token
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    // Find all active sessions (for auto-logout scheduler)
    List<UserSession> findByIsActiveTrue();
}