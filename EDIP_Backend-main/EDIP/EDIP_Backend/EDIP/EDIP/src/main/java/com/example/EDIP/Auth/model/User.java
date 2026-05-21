package com.example.EDIP.Auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "national_id", unique = true, nullable = false)
    private String nationalId;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    //  Login Security Fields
    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked")
    private boolean accountLocked = false;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    // Password Reset Control
    @Column(name = "reset_request_time")
    private LocalDateTime resetRequestTime;

    //  NEW: Email Verification Rate Limiting
    @Column(name = "last_verification_email_sent_at")
    private LocalDateTime lastVerificationEmailSentAt;

    // Department
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "organization_name")
    private String organizationName;
}