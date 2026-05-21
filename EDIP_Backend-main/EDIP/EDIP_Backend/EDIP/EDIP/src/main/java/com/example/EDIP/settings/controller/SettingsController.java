package com.example.EDIP.settings.controller;

import com.example.EDIP.settings.dto.*;
import com.example.EDIP.settings.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    // ✏ username
    @PutMapping("/username")
    public ApiResponse updateUsername(@RequestBody @Valid UpdateUsernameRequest request) {
        settingsService.updateUsername(request);
        return new ApiResponse("Username updated successfully",200);
    }

    //  organization
    @PutMapping("/organization")
    public ApiResponse updateOrganization(@RequestBody @Valid UpdateOrganizationRequest request) {
        settingsService.updateOrganization(request);
        return new ApiResponse("Organization updated successfully",200);
    }

    //  phone
    @PutMapping("/phone")
    public ApiResponse updatePhone(@RequestBody @Valid UpdatePhoneRequest request) {
        settingsService.updatePhone(request);
        return new ApiResponse("Phone updated successfully",200);
    }

    //  change password
    @PutMapping("/change-password")
    public ApiResponse changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        settingsService.changePassword(request);
        return new ApiResponse("Password changed successfully",200);
    }

    //  profile
    @GetMapping("/profile")
    public ProfileResponse getProfile() {
        return settingsService.getProfile();
    }
}