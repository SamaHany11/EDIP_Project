package com.example.EDIP.chatbot.dto;

import lombok.Data;

import java.util.UUID;
@Data
public class AskChatRequest {

    private String sessionId;
    private String documentId;
    private String question;

    private String switchDocumentId;
}
