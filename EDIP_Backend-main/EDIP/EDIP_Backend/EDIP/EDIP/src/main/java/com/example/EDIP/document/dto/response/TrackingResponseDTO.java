package com.example.EDIP.document.dto.response;

import com.example.EDIP.document.model.sql.DocumentStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class TrackingResponseDTO {
    private String status;
    private LocalDateTime changedAt;
    private boolean hasError;
    private String errorGuidance;
}