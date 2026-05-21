package com.example.EDIP.Auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "User_Sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * SHA-256 hash of the Access JWT token.
     * NEVER store the raw token — always hash before saving.
     */
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    /**
     * SHA-256 hash of the Refresh JWT token.
     * NEVER store the raw token — always hash before saving.
     */
    @Column(name = "refresh_token_hash", nullable = true)
    private String refreshTokenHash;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "logout_time", nullable = true)
    private LocalDateTime logoutTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "lastActivityTime", nullable = false)
    private LocalDateTime lastActivityTime;
}