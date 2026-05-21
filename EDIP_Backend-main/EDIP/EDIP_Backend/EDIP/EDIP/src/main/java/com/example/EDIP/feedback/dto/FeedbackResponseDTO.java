package com.example.EDIP.feedback.dto;

import com.example.EDIP.feedback.model.FeedbackStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FeedbackResponseDTO {

    private UUID feedbackId;
    private UUID documentId;
    private UUID departmentId;
    private String departmentName;
    private Integer rating;
    private String feedbackContent;
    private FeedbackStatus status;
    private LocalDateTime submittedAt;
}