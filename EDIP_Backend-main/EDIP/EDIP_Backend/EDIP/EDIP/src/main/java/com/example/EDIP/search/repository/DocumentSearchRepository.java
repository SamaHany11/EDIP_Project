package com.example.EDIP.search.repository;

import com.example.EDIP.document.model.sql.Document;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DocumentSearchRepository extends JpaRepository<Document, UUID> {

    // ========== EXTERNAL USER ==========
    /**
     * External users can see only documents they uploaded
     */
    @Query("""
        SELECT d FROM Document d
        WHERE d.isDeleted = false
        AND d.submittedBy = :userId
        AND LOWER(d.fileName) LIKE :keyword
        AND (:statuses IS NULL OR d.status IN :statuses)
        AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
        AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
    """)
    Page<Document> findDocumentsForExternal(
            @Param("userId")    UUID userId,
            @Param("keyword")   String keyword,
            @Param("statuses")  List<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );

    // ========== EMPLOYEE USER ==========
    /**
     * Employees can see:
     * - Documents they uploaded
     * - Documents assigned to them
     */
    @Query("""
        SELECT d FROM Document d
        WHERE d.isDeleted = false
        AND (d.submittedBy = :userId OR d.assignedTo = :userId)
        AND LOWER(d.fileName) LIKE :keyword
        AND (:statuses IS NULL OR d.status IN :statuses)
        AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
        AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
    """)
    Page<Document> findDocumentsForEmployee(
            @Param("userId")    UUID userId,
            @Param("keyword")   String keyword,
            @Param("statuses")  List<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );

    // ========== HEAD USER ==========
    /**
     * Heads can see:
     * - Documents they uploaded
     * - Documents assigned to them
     * - Documents in their department
     */
    @Query("""
        SELECT d FROM Document d
        WHERE d.isDeleted = false
        AND (
            d.submittedBy = :headId
            OR d.assignedTo = :headId
            OR d.currentDepartmentId = :departmentId
        )
        AND LOWER(d.fileName) LIKE :keyword
        AND (:statuses IS NULL OR d.status IN :statuses)
        AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
        AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
    """)
    Page<Document> findDocumentsForHead(
            @Param("headId")       UUID headId,
            @Param("departmentId") UUID departmentId,
            @Param("keyword")      String keyword,
            @Param("statuses")     List<String> statuses,
            @Param("startDate")    LocalDateTime startDate,
            @Param("endDate")      LocalDateTime endDate,
            Pageable pageable
    );

    // ========== ADMIN USER ==========
    /**
     * Admins can see all documents with optional filters:
     * - keyword: search in fileName
     * - statuses: filter by document status
     * - startDate / endDate: filter by submission date range
     * - departmentIds: filter by specific department UUIDs
     * - departmentName: filter by department name (partial match)
     *   → if both departmentIds and departmentName provided, both apply (AND)
     */
    @Query("""
        SELECT d FROM Document d
        WHERE d.isDeleted = false
        AND LOWER(d.fileName) LIKE :keyword
        AND (:statuses IS NULL OR d.status IN :statuses)
        AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
        AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
        AND (:departmentIds IS NULL OR d.currentDepartmentId IN :departmentIds)
        AND (:departmentName IS NULL OR LOWER(d.departmentName) LIKE :departmentName)
    """)
    Page<Document> findDocumentsForAdmin(
            @Param("keyword")        String keyword,
            @Param("statuses")       List<String> statuses,
            @Param("startDate")      LocalDateTime startDate,
            @Param("endDate")        LocalDateTime endDate,
            @Param("departmentIds")  List<UUID> departmentIds,
            @Param("departmentName") String departmentName,
            Pageable pageable
    );

    // ========== Legacy method for backward compatibility ==========
    @Query("""
        SELECT d FROM Document d
        WHERE d.isDeleted = false
        AND LOWER(d.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Document> searchAll(@Param("keyword") String keyword, Pageable pageable);


    // ========== HEAD SCOPED ==========

    @Query("""
    SELECT d FROM Document d
    WHERE d.isDeleted = false
    AND d.submittedBy = :headId
    AND LOWER(d.fileName) LIKE :keyword
    AND (:statuses IS NULL OR d.status IN :statuses)
    AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
""")
    Page<Document> findDocumentsUploadedByHead(
            @Param("headId")    UUID headId,
            @Param("keyword")   String keyword,
            @Param("statuses")  List<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    SELECT d FROM Document d
    WHERE d.isDeleted = false
    AND d.currentDepartmentId = :departmentId
    AND LOWER(d.fileName) LIKE :keyword
    AND (:statuses IS NULL OR d.status IN :statuses)
    AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
""")
    Page<Document> findDocumentsInHeadDepartment(
            @Param("departmentId") UUID departmentId,
            @Param("keyword")      String keyword,
            @Param("statuses")     List<String> statuses,
            @Param("startDate")    LocalDateTime startDate,
            @Param("endDate")      LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    SELECT d FROM Document d
    WHERE d.isDeleted = false
    AND d.assignedTo = :headId
    AND LOWER(d.fileName) LIKE :keyword
    AND (:statuses IS NULL OR d.status IN :statuses)
    AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
""")
    Page<Document> findDocumentsAssignedToHead(
            @Param("headId")    UUID headId,
            @Param("keyword")   String keyword,
            @Param("statuses")  List<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );

// ========== EMPLOYEE SCOPED ==========

    @Query("""
    SELECT d FROM Document d
    WHERE d.isDeleted = false
    AND d.submittedBy = :userId
    AND LOWER(d.fileName) LIKE :keyword
    AND (:statuses IS NULL OR d.status IN :statuses)
    AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
""")
    Page<Document> findDocumentsUploadedByEmployee(
            @Param("userId")    UUID userId,
            @Param("keyword")   String keyword,
            @Param("statuses")  List<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    SELECT d FROM Document d
    WHERE d.isDeleted = false
    AND d.assignedTo = :userId
    AND LOWER(d.fileName) LIKE :keyword
    AND (:statuses IS NULL OR d.status IN :statuses)
    AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
""")
    Page<Document> findDocumentsAssignedToEmployee(
            @Param("userId")    UUID userId,
            @Param("keyword")   String keyword,
            @Param("statuses")  List<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );

// ========== ADMIN SCOPED ==========

    @Query("""
    SELECT d FROM Document d
    WHERE d.isDeleted = false
    AND d.submittedBy = :adminId
    AND LOWER(d.fileName) LIKE :keyword
    AND (:statuses IS NULL OR d.status IN :statuses)
    AND (CAST(:startDate AS timestamp) IS NULL OR d.submittedDate >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR d.submittedDate <= :endDate)
""")
    Page<Document> findDocumentsUploadedByAdmin(
            @Param("adminId")   UUID adminId,
            @Param("keyword")   String keyword,
            @Param("statuses")  List<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );
}