package com.example.EDIP.document.model.sql;

import com.example.EDIP.chatbot.model.RagStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "Documents", indexes = {
        @Index(name = "IDX_Document_Status", columnList = "status"),
        @Index(name = "IDX_Document_Department", columnList = "current_department_id"),
        @Index(name = "IDX_Document_Submitter", columnList = "submitted_by"),
        @Index(name = "IDX_Document_AssignedTo", columnList = "assigned_to")
})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "document_format", nullable = false)
    private String documentFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.SUBMITTED;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "current_department_id")
    private UUID currentDepartmentId;

    @Column(name = "mongo_file_id")
    private String mongoFileId;

    @Column(name = "submitted_date", nullable = false, updatable = false)
    private LocalDateTime submittedDate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now().plusHours(3);
        this.submittedDate = now;
        this.lastUpdated = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.lastUpdated = LocalDateTime.now().plusHours(3);
    }

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "is_confidential", nullable = false)
    @NotNull
    private Boolean confidential;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_processing_status")
    private AiProcessingStatus aiProcessingStatus;

    @Column(name = "ai_retry_count")
    @Builder.Default
    private Integer aiRetryCount = 0;

    @Column(name = "file_name")
    private String fileName;


    @Enumerated(EnumType.STRING)
    @Column(name = "rag_status")
    private RagStatus ragStatus;
}