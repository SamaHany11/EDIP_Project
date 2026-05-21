package com.example.EDIP.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateEmployeeInfoRequest {

    @NotBlank
    @Pattern(
            regexp = "ADMIN|HEAD|EMPLOYEE",
            message = "ROLE_NOT_ALLOWED"
    )
    private String role;

    @NotBlank
    private String departmentName;
}