package com.example.EDIP.document.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReplyResponseDTO {

    private UUID replyId;
    private UUID documentId;

    private String replyText;
    private String fileName;
    private String fileType;
    private boolean hasFile;

    private String replyStatus;
    private String rejectionReason;

    private String createdByName;
    private String createdByEmail;


    private String rejectedByName;
    private String rejectedByEmail;
    private LocalDateTime rejectedAt;

    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
}