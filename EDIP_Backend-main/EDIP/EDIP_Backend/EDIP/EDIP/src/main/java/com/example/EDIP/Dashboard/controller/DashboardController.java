package com.example.EDIP.Dashboard.controller;

import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.Dashboard.dto.*;
import com.example.EDIP.Dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository   userRepository;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard(
            @RequestParam UUID departmentId,
            @RequestParam(defaultValue = "ALL") DateRange range,
            @RequestParam(defaultValue = "TOP") SortOrder sort,
            @RequestParam(required = false)     Integer year) {

        return ResponseEntity.ok(
                dashboardService.getAdminDashboard(departmentId, range, sort, year));
    }

    @GetMapping("/admin/departments/comparison")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentComparisonDTO> getDepartmentComparison(
            @RequestParam(defaultValue = "ALL") DateRange range,
            @RequestParam(defaultValue = "TOP") SortOrder sort,
            @RequestParam(required = false)     Integer year) {

        return ResponseEntity.ok(
                dashboardService.getDepartmentComparison(range, sort, year));
    }

    @GetMapping("/head")
    @PreAuthorize("hasRole('HEAD')")
    public ResponseEntity<HeadDashboardDTO> getHeadDashboard(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") DateRange range,
            @RequestParam(defaultValue = "TOP") SortOrder sort,
            @RequestParam(required = false)     Integer year) {

        String email = authentication.getName();
        UUID departmentId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getDepartmentId();

        return ResponseEntity.ok(
                dashboardService.getHeadDashboard(departmentId, range, sort, year));
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<EmployeeDashboardDTO> getEmployeeDashboard(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") DateRange range,
            @RequestParam(required = false)     Integer year) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                dashboardService.getEmployeeDashboard(email, range, year));
    }
}