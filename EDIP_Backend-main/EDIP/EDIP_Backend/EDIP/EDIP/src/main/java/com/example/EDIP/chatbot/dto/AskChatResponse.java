package com.example.EDIP.chatbot.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AskChatResponse {
    private String sessionId;
    private String question;
    private String answer;
}