package com.example.EDIP.chatbot.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    private String id;

    private String userId;

    private String title;

    private String ragDocumentId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}