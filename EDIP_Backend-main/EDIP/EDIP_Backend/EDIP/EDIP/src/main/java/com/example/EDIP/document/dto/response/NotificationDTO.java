package com.example.EDIP.document.dto.response;

import java.util.UUID;

import lombok.Data;

@Data
public class NotificationDTO {
    private UUID notificationId;
    private UUID documentId;
    private String notificationType;
    private String content;
    private String priority;
    private boolean read;
    private String createdAt;
}
