package com.example.EDIP.settings.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.DepartmentRepository;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.settings.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final DepartmentRepository departmentRepository;

    public SettingsService(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.departmentRepository = departmentRepository;
    }


    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }


    public void updateUsername(UpdateUsernameRequest request) {
        User user = getCurrentUser();

        // Role check → 403
        if (!user.getRole().equals("EXTERNAL")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only external users can change their username");
        }

        // Same username check → 400
        if (user.getUsername().equals(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New username must be different from current username");
        }


        user.setUsername(request.getUsername());
        userRepository.save(user);
    }


    public void updateOrganization(UpdateOrganizationRequest request) {
        User user = getCurrentUser();

        // Role check → 403
        if (!user.getRole().equals("EXTERNAL")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only external users can change their organization");
        }

        // Same organization check → 400
        if (request.getOrganizationName() != null
                && request.getOrganizationName().equals(user.getOrganizationName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New organization name must be different from current one");
        }

        user.setOrganizationName(request.getOrganizationName());
        userRepository.save(user);
    }

    // ============================
    // Update Phone
    // All users
    // ============================
    public void updatePhone(UpdatePhoneRequest request) {
        User user = getCurrentUser();

        // Same phone check → 400
        if (user.getPhoneNumber().equals(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New phone number must be different from current phone number");
        }

        // Phone already used by another account → 400
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is already in use by another account");
        }

        user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);
    }

    // ============================
    // Change Password
    // All users
    // ============================
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        // Old password must match the stored hash → 400
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }

        // New password must be different → 400
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from old password");
        }

        //  Encode new password as hash before saving
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // Reset security flags after password change
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setMustChangePassword(false);

        userRepository.save(user);
    }
    // ============================
//  Get Profile
// ============================
    public ProfileResponse getProfile() {

        User user = getCurrentUser();

        //  External
        if ("EXTERNAL".equals(user.getRole())) {

            return new ProfileResponse(
                    user.getUsername(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    null,
                    user.getOrganizationName(),
                    null
            );
        }

        // Internal
        String departmentName = null;

        if (user.getDepartmentId() != null) {
            departmentName = departmentRepository
                    .findById(user.getDepartmentId())
                    .map(dep -> dep.getDepartmentName())
                    .orElse(null);
        }

        return new ProfileResponse(
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                null,
                departmentName
        );
    }
}