package com.example.EDIP.Dashboard.repository;

import com.example.EDIP.Dashboard.repository.projection.DeptDocStatsProjection;
import com.example.EDIP.Dashboard.repository.projection.EmployeeStatsProjection;
import com.example.EDIP.Dashboard.repository.projection.StatusCountProjection;
import com.example.EDIP.Dashboard.repository.projection.TopPerformerProjection;
import com.example.EDIP.document.model.sql.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DashboardRepository extends JpaRepository<Document, UUID> {

    @Query("""
        SELECT d.status AS status, COUNT(d) AS count
        FROM Document d
        WHERE d.currentDepartmentId = :deptId
          AND d.isDeleted = false
          AND (:from IS NULL OR d.submittedDate >= :from)
          AND (:to   IS NULL OR d.submittedDate <= :to)
        GROUP BY d.status
    """)
    List<StatusCountProjection> countByStatusForDepartment(
            @Param("deptId") UUID deptId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    @Query("""
        SELECT
            u.id       AS userId,
            u.username AS username,
            u.email    AS email,
            COUNT(d)   AS assignedDocuments,
            SUM(CASE WHEN d.status = 'COMPLETED'   THEN 1L ELSE 0L END) AS completedDocuments,
            SUM(CASE WHEN d.status = 'PENDING'     THEN 1L ELSE 0L END) AS pendingDocuments,
            SUM(CASE WHEN d.status = 'IN_PROGRESS' THEN 1L ELSE 0L END) AS inProgressDocuments
        FROM User u
        LEFT JOIN Document d
            ON d.assignedTo = u.id
           AND d.isDeleted  = false
           AND (:from IS NULL OR d.submittedDate >= :from)
           AND (:to   IS NULL OR d.submittedDate <= :to)
        WHERE u.departmentId = :deptId
          AND u.role      = 'EMPLOYEE'
          AND u.isActive  = true
        GROUP BY u.id, u.username, u.email
        ORDER BY completedDocuments DESC
    """)
    List<EmployeeStatsProjection> findEmployeeStatsTop(
            @Param("deptId") UUID deptId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    @Query("""
        SELECT
            u.id       AS userId,
            u.username AS username,
            u.email    AS email,
            COUNT(d)   AS assignedDocuments,
            SUM(CASE WHEN d.status = 'COMPLETED'   THEN 1L ELSE 0L END) AS completedDocuments,
            SUM(CASE WHEN d.status = 'PENDING'     THEN 1L ELSE 0L END) AS pendingDocuments,
            SUM(CASE WHEN d.status = 'IN_PROGRESS' THEN 1L ELSE 0L END) AS inProgressDocuments
        FROM User u
        LEFT JOIN Document d
            ON d.assignedTo = u.id
           AND d.isDeleted  = false
           AND (:from IS NULL OR d.submittedDate >= :from)
           AND (:to   IS NULL OR d.submittedDate <= :to)
        WHERE u.departmentId = :deptId
          AND u.role      = 'EMPLOYEE'
          AND u.isActive  = true
        GROUP BY u.id, u.username, u.email
        ORDER BY completedDocuments ASC
    """)
    List<EmployeeStatsProjection> findEmployeeStatsBottom(
            @Param("deptId") UUID deptId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    @Query("""
        SELECT d.status AS status, COUNT(d) AS count
        FROM Document d
        WHERE d.assignedTo = :userId
          AND d.isDeleted  = false
          AND (:from IS NULL OR d.submittedDate >= :from)
          AND (:to   IS NULL OR d.submittedDate <= :to)
        GROUP BY d.status
    """)
    List<StatusCountProjection> countByStatusForEmployee(
            @Param("userId") UUID userId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    @Query("""
        SELECT
            d.currentDepartmentId AS departmentId,
            COUNT(d)              AS totalDocuments,
            SUM(CASE WHEN d.status = 'SUBMITTED'   THEN 1L ELSE 0L END) AS submitted,
            SUM(CASE WHEN d.status = 'IN_PROGRESS' THEN 1L ELSE 0L END) AS inProgress,
            SUM(CASE WHEN d.status = 'PENDING'     THEN 1L ELSE 0L END) AS pending,
            SUM(CASE WHEN d.status = 'COMPLETED'   THEN 1L ELSE 0L END) AS completed
        FROM Document d
        WHERE d.isDeleted = false
          AND (:from IS NULL OR d.submittedDate >= :from)
          AND (:to   IS NULL OR d.submittedDate <= :to)
        GROUP BY d.currentDepartmentId
    """)
    List<DeptDocStatsProjection> countDocsByAllDepartments(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("""
        SELECT
            u.departmentId AS departmentId,
            u.username     AS username,
            COUNT(d)       AS completedCount
        FROM User u
        LEFT JOIN Document d
            ON d.assignedTo = u.id
           AND d.status     = 'COMPLETED'
           AND d.isDeleted  = false
           AND (:from IS NULL OR d.submittedDate >= :from)
           AND (:to   IS NULL OR d.submittedDate <= :to)
        WHERE u.role     = 'EMPLOYEE'
          AND u.isActive = true
        GROUP BY u.departmentId, u.id, u.username
        ORDER BY u.departmentId, COUNT(d) DESC
    """)
    List<TopPerformerProjection> findTopPerformerPerDepartment(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);
}