package com.example.EDIP.document.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSummaryDTO {
    private UUID documentId;
    private String fileName;
    private int totalActions;
    private String lastAction;
    private String lastActionBy;
    private LocalDateTime lastActionTime;
}
