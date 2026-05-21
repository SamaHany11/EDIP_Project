package com.example.EDIP.Auth.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.Auth.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final EmailService emailService;

    public String createVerificationToken(User user) {

        String token = jwtUtil.generateVerificationToken(user.getEmail());

        emailService.sendVerificationEmail(user.getEmail(), token);


        user.setLastVerificationEmailSentAt(LocalDateTime.now());
        userRepository.save(user);

        return token;
    }

    public String verifyAccount(String token) {

        if (!jwtUtil.validateToken(token, "EMAIL_VERIFICATION")) {
            throw new RuntimeException("Invalid or expired token");
        }

        String email = jwtUtil.extractEmail(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            return "Account already activated";
        }

        user.setEnabled(true);
        userRepository.save(user);

        return "Account activated successfully";
    }

    public String resendVerification(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            return "Account already activated";
        }


        if (user.getLastVerificationEmailSentAt() != null) {

            Duration duration = Duration.between(
                    user.getLastVerificationEmailSentAt(),
                    LocalDateTime.now()
            );

            if (duration.toMinutes() < 5) {
                long remaining = 5 - duration.toMinutes();

                throw new RuntimeException(
                        "Please wait " + remaining + " minute(s) before requesting again"
                );
            }
        }


        String token = jwtUtil.generateVerificationToken(email);

        emailService.sendVerificationEmail(email, token);


        user.setLastVerificationEmailSentAt(LocalDateTime.now());
        userRepository.save(user);

        return "Verification email resent successfully";
    }
}