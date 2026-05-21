package com.example.EDIP.Auth.controller;

import com.example.EDIP.Auth.service.EmailVerificationService;
import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService verificationService;
    private final UserRepository userRepository;
    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam String token) {
        return ResponseEntity.ok(verificationService.verifyAccount(token));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) { // assuming 'isEnabled' is a field indicating whether the user account is active
            return ResponseEntity.badRequest().body("Email is already activated");
        }

        return ResponseEntity.ok(verificationService.resendVerification(email));
    }

}
