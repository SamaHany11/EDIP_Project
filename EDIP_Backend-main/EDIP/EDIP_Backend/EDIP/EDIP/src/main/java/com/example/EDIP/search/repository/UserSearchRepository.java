package com.example.EDIP.search.repository;

import com.example.EDIP.Auth.model.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface UserSearchRepository extends JpaRepository<User, UUID> {

    // ========== ADMIN USER ==========
    /**
     * Admin can search all active users with optional filters:
     * - keyword: search in username or email
     * - roleFilters: filter by role (HEAD, EMPLOYEE, etc.)
     * - departmentIds: filter by specific department UUIDs
     * - departmentName: filter by department name via subquery on Department entity
     *   → if both departmentIds and departmentName provided, both apply (AND)
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.isActive = true
        AND (LOWER(u.username) LIKE :keyword OR LOWER(u.email) LIKE :keyword)
        AND (:roleFilters IS NULL OR u.role IN :roleFilters)
        AND (:departmentIds IS NULL OR u.departmentId IN :departmentIds)
        AND (
            :departmentName IS NULL
            OR u.departmentId IN (
                SELECT d.id FROM Department d
                WHERE LOWER(d.departmentName) LIKE :departmentName
            )
        )
    """)
    Page<User> findUsersForAdmin(
            @Param("keyword")        String keyword,
            @Param("roleFilters")    List<String> roleFilters,
            @Param("departmentIds")  List<UUID> departmentIds,
            @Param("departmentName") String departmentName,
            Pageable pageable
    );

    // ========== HEAD USER ==========
    /**
     * Head can see:
     * - Users in their department
     * - All heads from other departments
     * - All admins
     * Optional filter by roleFilters (e.g. show EMPLOYEE only)
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.isActive = true
        AND (LOWER(u.username) LIKE :keyword OR LOWER(u.email) LIKE :keyword)
        AND (
            u.departmentId = :departmentId
            OR u.role = 'HEAD'
            OR u.role = 'ADMIN'
        )
        AND (:roleFilters IS NULL OR u.role IN :roleFilters)
        AND u.id != :excludeId
    """)
    Page<User> findUsersForHead(
            @Param("departmentId") UUID departmentId,
            @Param("keyword")      String keyword,
            @Param("roleFilters")  List<String> roleFilters,
            @Param("excludeId")    UUID excludeId,
            Pageable pageable
    );

    // ========== EMPLOYEE USER ==========
    /**
     * Employee can see only active users in their same department
     * Excludes themselves from results
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.isActive = true
        AND u.departmentId = :departmentId
        AND (LOWER(u.username) LIKE :keyword OR LOWER(u.email) LIKE :keyword)
        AND u.id != :excludeId
    """)
    Page<User> findUsersForEmployee(
            @Param("departmentId") UUID departmentId,
            @Param("keyword")      String keyword,
            @Param("excludeId")    UUID excludeId,
            Pageable pageable
    );

    // ========== Legacy method for backward compatibility ==========
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<User> searchAll(@Param("keyword") String keyword, Pageable pageable);

    // ========== Utility methods ==========
    User findByEmail(String email);
}