package com.example.EDIP.Dashboard.repository;

import com.example.EDIP.Auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserDashboardRepository extends JpaRepository<User, UUID> {


    List<User> findByDepartmentIdAndRoleAndIsActiveTrue(
            UUID departmentId, String role);


    long countByDepartmentIdAndRoleAndIsActiveTrue(
            UUID departmentId, String role);
}