package com.example.EDIP.Auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${BREVO_API_KEY}")
    private String apiKey;

    private final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    // ================= VERIFICATION EMAIL =================
    public void sendVerificationEmail(String toEmail, String token) {

        String verificationUrl = frontendUrl + "/verify?token=" + token;

        String body = "{\n" +
                "  \"sender\": {\"email\": \"shahdandsamabackendedipproject@gmail.com\"},\n" +
                "  \"to\": [{\"email\": \"" + toEmail + "\"}],\n" +
                "  \"subject\": \"Verify your account\",\n" +
                "  \"textContent\": \"Click the link below:\\n" + verificationUrl + "\"\n" +
                "}";

        sendRequest(body);
    }

    // ================= PASSWORD RESET EMAIL =================
    public void sendPasswordResetEmail(String toEmail, String token) {

        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String body = "{\n" +
                "  \"sender\": {\"email\": \"shahdandsamabackendedipproject@gmail.com\"},\n" +
                "  \"to\": [{\"email\": \"" + toEmail + "\"}],\n" +
                "  \"subject\": \"Reset Your Password\",\n" +
                "  \"textContent\": \"Click the link below:\\n" + resetUrl + "\"\n" +
                "}";

        sendRequest(body);
    }

    // ================= COMMON METHOD =================
    private void sendRequest(String body) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(BREVO_URL, request, String.class);
    }
}
