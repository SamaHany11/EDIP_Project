package com.example.EDIP.document.repository.sql;
import com.example.EDIP.document.model.sql.Attachment;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.document.model.sql.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByDocumentId(UUID documentId);
}