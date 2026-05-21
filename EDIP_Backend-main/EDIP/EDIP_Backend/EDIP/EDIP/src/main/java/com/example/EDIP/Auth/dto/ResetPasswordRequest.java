package com.example.EDIP.Auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = ".*[A-Z].*", message = "Must contain uppercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Must contain number")
    @Pattern(regexp = ".*[!@#$%^&*()].*", message = "Must contain special character")
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
