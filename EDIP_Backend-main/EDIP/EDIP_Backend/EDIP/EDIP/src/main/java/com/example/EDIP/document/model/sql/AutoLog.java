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
@Table(name = "Audit_Log", indexes = {
        @Index(name = "IDX_AutoLog_Document",  columnList = "document_id"),
        @Index(name = "IDX_AutoLog_User",      columnList = "user_id"),
        @Index(name = "IDX_AutoLog_Timestamp", columnList = "action_timestamp")
})
public class AutoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "document_id")
    private UUID documentId;

    /*
     * DOCUMENT_SUBMITTED / FILE_UPLOADED / OCR_PROCESSED
     * CLASSIFICATION_DONE / DOCUMENT_ROUTED
     * ASSIGNED_TO_EMPLOYEE / FORWARDED_TO_DEPARTMENT
     * FORWARDED_TO_COLLEAGUE / REPLY_CREATED
     * REPLY_APPROVED / REPLY_REJECTED / REPLY_SENT
     */
    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "document_status")
    private String documentStatus;



    @Column(name = "action_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime actionTimestamp = LocalDateTime.now();

    @Column(name = "action_label")
    private String actionLabel;
}