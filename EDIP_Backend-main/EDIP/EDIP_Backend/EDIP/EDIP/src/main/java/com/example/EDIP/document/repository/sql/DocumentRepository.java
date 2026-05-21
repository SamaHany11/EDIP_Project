package com.example.EDIP.document.repository.sql;

import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.document.model.sql.DocumentStatus;
import com.example.EDIP.document.model.sql.AiProcessingStatus;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {


    Page<Document> findBySubmittedByAndIsDeletedFalse(
            UUID submittedBy, Pageable pageable);



    Page<Document> findByCurrentDepartmentIdAndIsDeletedFalse(
            UUID departmentId, Pageable pageable);


    Page<Document> findByCurrentDepartmentIdAndStatusAndIsDeletedFalse(
            UUID departmentId, DocumentStatus status, Pageable pageable);


    Page<Document> findByAssignedToAndIsDeletedFalse(
            UUID assignedTo, Pageable pageable);


    Optional<Document> findByDocumentIdAndIsDeletedFalse(UUID documentId);


    Optional<Document> findByDocumentIdAndSubmittedByAndIsDeletedFalse(
            UUID documentId, UUID submittedBy);


    Optional<Document> findByDocumentIdAndCurrentDepartmentIdAndIsDeletedFalse(
            UUID documentId, UUID departmentId);

    List<Document> findByAiProcessingStatusAndAiRetryCountLessThanAndConfidentialFalseAndIsDeletedFalse(
            AiProcessingStatus status,
            int retryCount
    );

    Page<Document> findBySubmittedByAndConfidentialTrueAndIsDeletedFalse(
            UUID submittedBy, Pageable pageable);

    Page<Document> findBySubmittedByAndConfidentialFalseAndIsDeletedFalse(
            UUID submittedBy, Pageable pageable);

    @Query("""
            SELECT d FROM Document d
            WHERE d.isDeleted = false
            AND d.confidential = true
            AND (
                (d.targetUserId = :userId)
                OR (d.submittedBy = :userId)
            )
            """)
    Page<Document> findConfidentialDocsForUser(UUID userId, Pageable pageable);

    Page<Document> findByCurrentDepartmentIdAndConfidentialFalseAndIsDeletedFalse(
            UUID departmentId,
            Pageable pageable
    );
}