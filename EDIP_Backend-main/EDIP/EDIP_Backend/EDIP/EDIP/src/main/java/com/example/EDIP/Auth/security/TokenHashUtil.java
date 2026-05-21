package com.example.EDIP.Auth.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for hashing tokens before storing them in the database.
 * Uses SHA-256 to ensure tokens are never stored in plain text.
 */
@Component
public class TokenHashUtil {

    /**
     * Hashes a token using SHA-256 before DB storage.
     * @param token the plain JWT token
     * @return Base64-encoded SHA-256 hash of the token
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}