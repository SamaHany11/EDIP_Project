package com.example.EDIP.settings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private String username;
    private String email;
    private String phoneNumber;

    private String role;
    private String organizationName;
    private String departmentName;
}