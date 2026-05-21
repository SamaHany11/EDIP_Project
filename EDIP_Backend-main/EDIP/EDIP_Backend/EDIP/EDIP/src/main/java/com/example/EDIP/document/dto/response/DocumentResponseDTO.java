package com.example.EDIP.document.dto.response;

import com.example.EDIP.document.model.sql.DocumentStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DocumentResponseDTO {

    private UUID documentId;


    private String documentFormat;
    private String status;
    private LocalDateTime submittedDate;
    private LocalDateTime lastUpdated;
    private String fileName;
    private String priority;
    private Boolean confidential;
    private String latestReplyStatus;

    private UUID latestReplyId;



    private String rejectionReason;

    private List<AttachmentDTO> attachments;
}