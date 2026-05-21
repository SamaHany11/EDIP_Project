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
@Table(name = "Notifications", indexes = {
        @Index(name = "IDX_Notification_User",     columnList = "user_id"),
        @Index(name = "IDX_Notification_Read",     columnList = "is_read"),
        @Index(name = "IDX_Notification_Document", columnList = "document_id")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "document_id")
    private UUID documentId;

    /*
     * DOCUMENT_RECEIVED / DOCUMENT_ASSIGNED
     * REPLY_PENDING_APPROVAL / REPLY_APPROVED
     * REPLY_REJECTED / DOCUMENT_COMPLETED
     */
    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    // HIGH / MEDIUM / LOW
    @Column(name = "priority")
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}