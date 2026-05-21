package com.example.EDIP.Auth.service;

import com.example.EDIP.Auth.dto.AuthResponse;
import com.example.EDIP.Auth.dto.LoginRequest;
import com.example.EDIP.Auth.dto.RegisterRequest;
import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.model.UserSession;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.Auth.repository.UserSessionRepository;
import com.example.EDIP.Auth.security.JWTUtil;
import com.example.EDIP.Auth.security.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class
AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final TokenHashUtil tokenHashUtil;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;


    // ─────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────

    public String register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and Confirm Password do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists!");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists!");
        }

        if (userRepository.existsByNationalId(request.getNationalId())) {
            throw new IllegalArgumentException("National ID already exists!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNationalId(request.getNationalId());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole("EXTERNAL");
        user.setEnabled(false);
        user.setIsActive(true);
        user.setOrganizationName(request.getOrganizationName());
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        emailVerificationService.createVerificationToken(user);

        return "User registered successfully. Please check your email.";
    }

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────


    public AuthResponse login(LoginRequest loginRequest,
                              HttpServletRequest request) {

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));


        if (user.isAccountLocked()) {


            if (isLockTimeExpired(user)) {
                unlockAccount(user);
            } else {
                throw new IllegalArgumentException(
                        "Account locked due to multiple failed attempts. Try again after 15 minutes."
                );
            }
        }


        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {


            incrementFailedAttempts(user);


            if (user.getFailedLoginAttempts() >= 5) {
                throw new IllegalArgumentException(
                        "Account locked due to multiple failed attempts. Try again after 15 minutes."
                );
            }

            throw new IllegalArgumentException("Invalid email or password");
        }


        resetFailedAttempts(user);


        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is not verified");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is inactive");
        }


        String accessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole()
        );

        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());


        String hashedAccessToken = tokenHashUtil.hashToken(accessToken);
        String hashedRefreshToken = tokenHashUtil.hashToken(refreshToken);
        String passwordResetToken = null;

        if (user.isMustChangePassword()) {

            passwordResetToken = jwtUtil.generatePasswordResetToken(user.getEmail());

            user.setResetRequestTime(LocalDateTime.now());
            userRepository.save(user);
        }

        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setTokenHash(hashedAccessToken);
        session.setRefreshTokenHash(hashedRefreshToken);
        session.setLoginTime(LocalDateTime.now());
        session.setLastActivityTime(LocalDateTime.now());
        session.setIsActive(true);

        userSessionRepository.save(session);

        return new AuthResponse(
                "Login successful",
                accessToken,
                refreshToken,
                user.isMustChangePassword(),
                passwordResetToken
        );
    }


    // ─────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────

    public AuthResponse refreshAccessToken(String refreshToken) {

        // 1. Validate the refresh token signature and type
        if (!jwtUtil.validateToken(refreshToken, "REFRESH")) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        // 2. Check if the hashed refresh token exists in DB
        String hashedRefreshToken = tokenHashUtil.hashToken(refreshToken);
        UserSession session = userSessionRepository
                .findByRefreshTokenHash(hashedRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // 3. Ensure session is still active
        if (!session.getIsActive()) {
            throw new IllegalArgumentException("Session has been terminated");
        }
        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String newAccessToken = jwtUtil.generateToken(email, user.getRole());

        // 4. Issue a new Access Token




        // 5. Hash and update the access token in DB
        session.setTokenHash(tokenHashUtil.hashToken(newAccessToken));
        session.setLastActivityTime(LocalDateTime.now());
        userSessionRepository.save(session);

        // Return only the new access token (refresh token stays the same)
        return new AuthResponse(
                "Token refreshed successfully",
                newAccessToken,
                refreshToken,
                user.isMustChangePassword(),
                null
        );
    }

    // ─────────────────────────────────────────────
    // AUTO LOGOUT (inactivity check every 60s)
    // ─────────────────────────────────────────────

    @Scheduled(fixedRate = 60000)
    public void scheduleAutoLogoutCheck() {
        checkForAutoLogout();
    }

    public void checkForAutoLogout() {
        List<UserSession> activeSessions = userSessionRepository.findByIsActiveTrue();
        for (UserSession session : activeSessions) {
            if (isSessionExpired(session)) {
                session.setIsActive(false);
                session.setLogoutTime(LocalDateTime.now());
                userSessionRepository.save(session);
            }
        }
    }

    private boolean isSessionExpired(UserSession session) {
        return session.getLastActivityTime()
                .plusMinutes(480)
                .isBefore(LocalDateTime.now());
    }

    // ─────────────────────────────────────────────
    // ACCOUNT LOCK
    // ─────────────────────────────────────────────

    private void incrementFailedAttempts(User user) {
        int failedAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(failedAttempts);
        if (failedAttempts >= 5) {
            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);
    }

    private boolean isLockTimeExpired(User user) {
        return user.getLockTime() != null &&
                user.getLockTime().plusMinutes(15).isBefore(LocalDateTime.now());
    }

    private void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);
    }

    // ─────────────────────────────────────────────
    // FORGOT / RESET PASSWORD
    // ─────────────────────────────────────────────

    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not registered"));
        checkResetRateLimit(user);
        String token = jwtUtil.generatePasswordResetToken(email);
        user.setResetRequestTime(LocalDateTime.now());
        userRepository.save(user);
        emailService.sendPasswordResetEmail(email, token);
        return "Reset link sent successfully.";
    }

    public String resetPassword(String token, String newPassword, String confirmPassword) {

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and Confirm Password do not match");
        }
        if (!jwtUtil.validateToken(token, "PASSWORD_RESET")) {
            throw new IllegalArgumentException("Reset link expired or invalid");
        }
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);
        return "Password reset successful.";
    }

    public String resendResetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not registered"));
        checkResetRateLimit(user);
        String token = jwtUtil.generatePasswordResetToken(email);

        user.setResetRequestTime(LocalDateTime.now());
        userRepository.save(user);
        emailService.sendPasswordResetEmail(email, token);
        return "New reset link sent successfully.";
    }

    private void checkResetRateLimit(User user) {
        if (user.getResetRequestTime() != null &&
                user.getResetRequestTime().plusMinutes(5).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Reset link already sent. Please wait 5 minutes before requesting again.");
        }
    }
}