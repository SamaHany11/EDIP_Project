package com.example.EDIP.Auth.repository;

import com.example.EDIP.Auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByNationalId(String nationalId);



    Optional<User> findByDepartmentIdAndRole(UUID departmentId, String role);
    Page<User> findByDepartmentId(UUID departmentId, Pageable pageable);

    default Optional<User> findHeadByDepartmentId(UUID departmentId) {
        return findByDepartmentIdAndRole(departmentId, "HEAD");
    }




}

