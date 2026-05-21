package com.example.EDIP.Auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username is mandatory")
    private String username;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit")
    @Pattern(regexp = ".*[!@#$%^&*()].*", message = "Must contain special character")
    private String password;

    @NotBlank(message = "Confirm Password is mandatory")
    private String confirmPassword;

    @NotBlank(message = "National ID is mandatory")
    @Size(min = 14, max = 14, message = "National ID must be exactly 14 digits")
    private String nationalId;

    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "\\d{11}", message = "Phone number must be exactly 11 digits")
    private String phoneNumber;

    @NotBlank(message = "Organization name is mandatory")
    private String organizationName;
}
