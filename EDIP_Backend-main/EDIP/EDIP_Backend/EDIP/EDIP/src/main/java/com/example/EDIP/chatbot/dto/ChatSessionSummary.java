package com.example.EDIP.chatbot.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionSummary {
    private String sessionId;
    private String title;
    private String ragDocumentId;
    private LocalDateTime updatedAt;
    private String lastMessage;
}