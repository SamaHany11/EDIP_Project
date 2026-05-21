package com.example.EDIP.account.controller;

import com.example.EDIP.Auth.model.Department;
import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.DepartmentRepository;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.account.dto.UpdateEmployeeInfoRequest;
import com.example.EDIP.account.service.AccountAccessService;
import com.example.EDIP.account.dto.CreateEmployeeAccountRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;

import java.util.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountAccessService accountAccessService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;



    // ─────────────────────────────────────────────
    // GET ALL USERS
    // ─────────────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String department
    ) {

        if (!accountAccessService.canManageAccounts()) {
            return ResponseEntity.status(403).body(
                    Map.of("message", "ACCESS_DENIED")
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<User> users;

        if (department != null && !department.isBlank()) {

            Department dept = departmentRepository
                    .findByDepartmentNameIgnoreCase(department)
                    .orElseThrow(() -> new RuntimeException("DEPARTMENT_NOT_FOUND"));

            users = userRepository.findByDepartmentId(dept.getDepartmentId(), pageable);

        } else {
            users = userRepository.findAll(pageable);
        }

        return ResponseEntity.ok(
                Map.of(
                        "message", "USERS_RETRIEVED_SUCCESSFULLY",
                        "data", users
                )
        );
    }

    // ─────────────────────────────────────────────
    // DISABLE USER
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable UUID id) {

        if (!accountAccessService.canManageAccounts()) {
            return ResponseEntity.status(403).body(
                    Map.of("message", "ACCESS_DENIED")
            );
        }

        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "USER_NOT_FOUND")
            );
        }

        User user = userOpt.get();
        user.setEnabled(false);
        user.setIsActive(false);
        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "USER_DISABLED_SUCCESSFULLY")
        );
    }

    // ─────────────────────────────────────────────
    // ENABLE USER
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable UUID id) {

        if (!accountAccessService.canManageAccounts()) {
            return ResponseEntity.status(403).body(
                    Map.of("message", "ACCESS_DENIED")
            );
        }

        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "USER_NOT_FOUND")
            );
        }

        User user = userOpt.get();
        user.setEnabled(true);
        user.setIsActive(true);
        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "USER_ENABLED_SUCCESSFULLY")
        );
    }

    // ─────────────────────────────────────────────
    // CREATE USER
    // ─────────────────────────────────────────────
    @PostMapping("/create")
    public ResponseEntity<?> createEmployeeAccount(
            @Valid @RequestBody CreateEmployeeAccountRequest request
    ) {

        if (!accountAccessService.canManageAccounts()) {
            return ResponseEntity.status(403).body(
                    Map.of("message", "ACCESS_DENIED")
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "EMAIL_ALREADY_EXISTS")
            );
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "PHONE_ALREADY_EXISTS")
            );
        }

        if (userRepository.existsByNationalId(request.getNationalId())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "NATIONAL_ID_ALREADY_EXISTS")
            );
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setNationalId(request.getNationalId());

        user.setRole(request.getRole());

        Department department = departmentRepository
                .findByDepartmentNameIgnoreCase(request.getDepartmentName())
                .orElseThrow(() -> new RuntimeException("DEPARTMENT_NOT_FOUND"));

        user.setDepartmentId(department.getDepartmentId());

        user.setPasswordHash(
                passwordEncoder.encode(request.getTemporaryPassword())
        );

        user.setEnabled(true);
        user.setIsActive(true);
        user.setMustChangePassword(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "EMPLOYEE_CREATED_SUCCESSFULLY")
        );
    }

    // ─────────────────────────────────────────────
    // UPDATE USER
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/update-info")
    public ResponseEntity<?> updateEmployeeInfo(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeInfoRequest request
    ) {

        accountAccessService.updateEmployeeInfo(id, request);

        return ResponseEntity.ok(
                Map.of("message", "EMPLOYEE_UPDATED_SUCCESSFULLY")
        );
    }
}