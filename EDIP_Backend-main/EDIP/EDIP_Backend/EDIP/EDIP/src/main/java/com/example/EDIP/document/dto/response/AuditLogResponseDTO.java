package com.example.EDIP.document.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {
    private UUID documentId;
    private String actionLabel;
    private String performedBy;
    private String departmentName;
    private String documentStatus;
    private String details;
    private LocalDateTime timestamp;
}