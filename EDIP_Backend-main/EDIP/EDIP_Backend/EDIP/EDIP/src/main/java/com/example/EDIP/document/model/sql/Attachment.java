package com.example.EDIP.document.model.sql;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "Attachments")
@Getter
@Setter
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attachment_id")
    private UUID attachmentId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mongo_file_id")
    private String mongoFileId;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();


}