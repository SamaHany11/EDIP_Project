package com.example.EDIP.feedback.repository;

import com.example.EDIP.feedback.model.Feedback;
import com.example.EDIP.feedback.model.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    boolean existsByDocumentId(UUID documentId);

    @Query("""
        SELECT f FROM Feedback f
        JOIN Document d ON f.documentId = d.documentId
        WHERE d.currentDepartmentId = :departmentId
        ORDER BY f.submittedAt DESC
    """)
    Page<Feedback> findByDepartmentId(
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );

    @Query("""
        SELECT f FROM Feedback f
        JOIN Document d ON f.documentId = d.documentId
        WHERE d.currentDepartmentId = :departmentId
          AND f.status = :status
        ORDER BY f.submittedAt DESC
    """)
    Page<Feedback> findByDepartmentIdAndStatus(
            @Param("departmentId") UUID departmentId,
            @Param("status") FeedbackStatus status,
            Pageable pageable
    );
}