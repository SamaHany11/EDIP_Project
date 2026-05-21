package com.example.EDIP.Auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private String accessToken;
    private String refreshToken;
    private Boolean mustChangePassword;
    private String passwordResetToken;


    public AuthResponse(String message) {
        this.message = message;
    }
}
