package com.example.EDIP.settings.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUsernameRequest {

    @NotEmpty(message = "Username cannot be empty")
    private String username;
}