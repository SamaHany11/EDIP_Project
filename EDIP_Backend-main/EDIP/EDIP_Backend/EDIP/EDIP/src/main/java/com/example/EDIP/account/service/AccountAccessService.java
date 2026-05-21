package com.example.EDIP.account.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.Auth.model.Department;
import com.example.EDIP.Auth.repository.DepartmentRepository;
import com.example.EDIP.account.dto.UpdateEmployeeInfoRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Service
@RequiredArgsConstructor
public class AccountAccessService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public boolean canManageAccounts() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) return false;

        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        Department itDepartment = departmentRepository
                .findByDepartmentNameIgnoreCase("it_department")
                .orElse(null);

        if (itDepartment == null) return false;

        return itDepartment.getDepartmentId().equals(user.getDepartmentId());
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    }

    @Transactional
    public void updateEmployeeInfo(UUID targetUserId, UpdateEmployeeInfoRequest request) {

        User currentUser = getCurrentUser();

        if (!canManageAccounts()) {
            throw new RuntimeException("ACCESS_DENIED");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));


        if (currentUser.getId().equals(targetUserId)
                && !"ADMIN".equals(currentUser.getRole())
                && !canManageAccounts()) {

            throw new RuntimeException("SELF_MODIFICATION_NOT_ALLOWED");
        }

        if ("EXTERNAL".equals(targetUser.getRole())) {
            throw new RuntimeException("ROLE_NOT_ALLOWED");
        }

        Department department = departmentRepository
                .findByDepartmentNameIgnoreCase(request.getDepartmentName())
                .orElseThrow(() -> new RuntimeException("DEPARTMENT_NOT_FOUND"));

        targetUser.setRole(request.getRole());
        targetUser.setDepartmentId(department.getDepartmentId());

        userRepository.save(targetUser);
    }
}