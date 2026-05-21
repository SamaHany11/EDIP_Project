package com.example.EDIP.chatbot.controller;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.chatbot.dto.AskChatRequest;
import com.example.EDIP.chatbot.dto.AskChatResponse;
import com.example.EDIP.chatbot.model.ChatSession;
import com.example.EDIP.chatbot.service.ChatbotService;
import com.example.EDIP.chatbot.dto.ChatSessionSummary;
import com.example.EDIP.chatbot.model.ChatMessage;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    // =========================
    // ASK QUESTION
    // =========================
    @PostMapping("/ask")
    public ResponseEntity<AskChatResponse> ask(
            @RequestBody AskChatRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                chatbotService.ask(authentication.getName(), request)
        );
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AskChatResponse> uploadAndAsk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question,
            @RequestParam(value = "sessionId", required = false)
            String sessionId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                chatbotService.uploadAndAsk(
                        authentication.getName(),
                        file,
                        question,
                        sessionId
                )
        );
    }


    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionSummary>> getSessions(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                chatbotService.getUserSessions(authentication.getName())
        );
    }


    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                chatbotService.getSessionMessages(
                        authentication.getName(),
                        sessionId
                )
        );
    }


    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteSession(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        chatbotService.deleteSession(
                authentication.getName(),
                sessionId
        );
        return ResponseEntity.ok(
                Map.of("message", "Session deleted successfully")
        );
    }
}