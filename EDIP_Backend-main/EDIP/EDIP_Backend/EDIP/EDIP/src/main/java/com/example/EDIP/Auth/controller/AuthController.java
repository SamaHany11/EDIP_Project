package com.example.EDIP.Auth.controller;

import com.example.EDIP.Auth.dto.RegisterRequest;
import com.example.EDIP.Auth.dto.ResetPasswordRequest;
import com.example.EDIP.Auth.dto.LoginRequest;
import com.example.EDIP.Auth.dto.AuthResponse;
import com.example.EDIP.Auth.service.AuthService;
import com.example.EDIP.Auth.repository.UserSessionRepository;
import com.example.EDIP.Auth.model.UserSession;
import com.example.EDIP.Auth.security.TokenHashUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final UserSessionRepository userSessionRepository;
    private final TokenHashUtil tokenHashUtil;

    // ─────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {

            String msg = e.getMessage().toLowerCase();
            if (msg.contains("email")) {
                return ResponseEntity.status(409).body(e.getMessage());
            } else if (msg.contains("phone")) {
                return ResponseEntity.status(409).body(e.getMessage());
            } else if (msg.contains("password")) {
                return ResponseEntity.status(422).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request) {
        try {
            AuthResponse authResponse = authService.login(loginRequest, request);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {

            String msg = e.getMessage().toLowerCase();
            if (msg.contains("email") || msg.contains("password")) {
                return ResponseEntity.badRequest().body(new AuthResponse(e.getMessage()));
            }
            return ResponseEntity.badRequest().body(new AuthResponse(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // REFRESH ACCESS TOKEN
    // ─────────────────────────────────────────────
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            return ResponseEntity.status(401).body("missing token");
        }

        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("invalid token");
        }

        String refreshToken = authHeader.substring(7);

        try {
            AuthResponse response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body("invalid token");
        }
    }

    // ─────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid token");
        }

        String token = authHeader.substring(7);

        //  Hash the token before searching in DB
        String hashedToken = tokenHashUtil.hashToken(token);

        Optional<UserSession> sessionOpt = userSessionRepository.findByTokenHash(hashedToken);

        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setIsActive(false);
            session.setLogoutTime(LocalDateTime.now());
            userSessionRepository.save(session);
            return ResponseEntity.ok("Logout successful");
        }

        return ResponseEntity.badRequest().body("Invalid token");
    }

    // ─────────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            return ResponseEntity.ok(authService.forgotPassword(email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // RESET PASSWORD
    // ─────────────────────────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            return ResponseEntity.ok(
                    authService.resetPassword(
                            request.getToken(),
                            request.getNewPassword(),
                            request.getConfirmPassword()
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // RESEND RESET PASSWORD
    // ─────────────────────────────────────────────
    @PostMapping("/resend-reset-password")
    public ResponseEntity<?> resendResetPassword(@RequestParam String email) {
        try {
            return ResponseEntity.ok(authService.resendResetPassword(email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}