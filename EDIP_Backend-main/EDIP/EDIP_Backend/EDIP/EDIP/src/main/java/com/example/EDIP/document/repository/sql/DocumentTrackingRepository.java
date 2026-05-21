package com.example.EDIP.document.repository.sql;

import com.example.EDIP.document.model.sql.DocumentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentTrackingRepository
        extends JpaRepository<DocumentTracking, UUID> {


    List<DocumentTracking> findByDocumentIdOrderByChangedAtAsc(UUID documentId);

    DocumentTracking findTopByDocumentIdOrderByChangedAtDesc(UUID documentId);
}