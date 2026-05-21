package com.example.EDIP.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePhoneRequest {

    @NotBlank
    @Pattern(regexp = "^01[0-9]{9}$", message = "Phone must be 11 digits and valid Egyptian number")
    private String phoneNumber;
}