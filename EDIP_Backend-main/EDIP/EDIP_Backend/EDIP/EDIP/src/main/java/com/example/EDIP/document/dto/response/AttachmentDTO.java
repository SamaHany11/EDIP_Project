package com.example.EDIP.document.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AttachmentDTO {
    private UUID attachmentId;
    private String fileName;
    private String fileType;
    private Long fileSize;
}
