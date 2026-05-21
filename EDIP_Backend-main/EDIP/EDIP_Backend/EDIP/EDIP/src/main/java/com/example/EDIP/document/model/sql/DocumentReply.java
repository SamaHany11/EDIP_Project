package com.example.EDIP.document.model.sql;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Document_Replies", indexes = {
        @Index(name = "IDX_Reply_Document", columnList = "document_id")
})
public class DocumentReply {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reply_id")
    private UUID replyId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;


    @Column(name = "reply_text", length = 2000)
    private String replyText;


    @Column(name = "mongo_file_id")
    private String mongoFileId;

    @Column(name = "file_name")
    private String fileName;

    // PDF / IMAGE / WORD
    @Column(name = "file_type")
    private String fileType;


    @Column(name = "prepared_by", nullable = false)
    private UUID preparedBy;


    @Column(name = "approved_by")
    private UUID approvedBy;


    @Enumerated(EnumType.STRING)
    @Column(name = "reply_status", nullable = false)
    @Builder.Default
    private ReplyStatus replyStatus = ReplyStatus.PENDING_APPROVAL;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reply_type")
    @Enumerated(EnumType.STRING)
    private ReplyType replyType;

    @Column(name = "final_signed_mongo_file_id")
    private String finalSignedMongoFileId;

    @Column(name = "final_signed_file_name")
    private String finalSignedFileName;

    @Column(name = "final_signed_file_type")
    private String finalSignedFileType;
}