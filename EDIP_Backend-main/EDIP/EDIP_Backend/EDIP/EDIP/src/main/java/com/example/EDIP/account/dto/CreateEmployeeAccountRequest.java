package com.example.EDIP.account.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateEmployeeAccountRequest {

    @NotBlank(message = "Username is mandatory")
    private String username;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "\\d{11}", message = "Phone number must be exactly 11 digits")
    private String phoneNumber;

    @NotBlank(message = "National ID is mandatory")
    @Size(min = 14, max = 14, message = "National ID must be exactly 14 digits")
    private String nationalId;

    @NotNull(message = "Role is mandatory")
    @Pattern(
            regexp = "ADMIN|HEAD|EMPLOYEE|EXTERNAL",
            message = "Invalid role"
    )
    private String role;

    @NotBlank(message = "Department is mandatory")
    private String departmentName;

    @NotBlank(message = "Temporary password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit")
    @Pattern(regexp = ".*[!@#$%^&*()].*", message = "Password must contain special character")
    private String temporaryPassword;
}