package com.example.EDIP.chatbot.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.chatbot.client.RagClient;
import com.example.EDIP.chatbot.dto.AskChatRequest;
import com.example.EDIP.chatbot.dto.AskChatResponse;
import com.example.EDIP.chatbot.dto.ChatSessionSummary;
import com.example.EDIP.chatbot.model.ChatMessage;
import com.example.EDIP.chatbot.model.ChatSession;
import com.example.EDIP.chatbot.model.RagStatus;
import com.example.EDIP.chatbot.repository.ChatMessageRepository;
import com.example.EDIP.chatbot.repository.ChatSessionRepository;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.document.repository.sql.DocumentRepository;
import com.example.EDIP.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final RagClient ragClient;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final RagProcessingService ragProcessingService;

    // =========================
    // ASK CHAT
    // =========================
    public AskChatResponse ask(
            String email,
            AskChatRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        ChatSession session;

        // =====================================
        // EXISTING SESSION
        // =====================================
        if (request.getSessionId() != null &&
                !request.getSessionId().isBlank()) {

            session = sessionRepo.findByIdAndUserId(
                    request.getSessionId(),
                    user.getId().toString()
            ).orElseThrow(() ->
                    new RuntimeException("Session not found"));


            if (request.getSwitchDocumentId() != null &&
                    !request.getSwitchDocumentId().isBlank()) {

                Document newDoc = documentRepository
                        .findByDocumentIdAndIsDeletedFalse(
                                UUID.fromString(request.getSwitchDocumentId())
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Document not found"));

                documentService.validateAccess(newDoc, user);

                if (newDoc.getRagStatus() != RagStatus.DONE) {
                    ragProcessingService.uploadToRagForNewUser(
                            newDoc.getDocumentId(),
                            user.getId().toString()
                    );
                    throw new RuntimeException(
                            "Document is being prepared, please try again in a moment."
                    );
                }


                session.setRagDocumentId(newDoc.getDocumentId().toString());
                session.setTitle(newDoc.getFileName());
                session.setUpdatedAt(LocalDateTime.now());
                sessionRepo.save(session);
            }
        }

        // =====================================
        // CHAT WITH DOCUMENT
        // =====================================
        else if (request.getDocumentId() != null &&
                !request.getDocumentId().isBlank()) {

            Document document = documentRepository
                    .findByDocumentIdAndIsDeletedFalse(
                            UUID.fromString(request.getDocumentId())
                    )
                    .orElseThrow(() ->
                            new RuntimeException("Document not found"));

            documentService.validateAccess(document, user);

            session = ChatSession.builder()
                    .userId(user.getId().toString())
                    .title(document.getFileName())
                    .ragDocumentId(
                            document.getDocumentId().toString()
                    )
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            sessionRepo.save(session);
        }

        // =====================================
        // INVALID REQUEST
        // =====================================
        else {

            throw new RuntimeException(
                    "documentId or sessionId is required"
            );
        }

        // =====================================
        // RAG READY CHECK
        // =====================================
        String ragId = session.getRagDocumentId();

        try {

            UUID documentUUID = UUID.fromString(ragId);

            Document document = documentRepository
                    .findByDocumentIdAndIsDeletedFalse(
                            documentUUID
                    )
                    .orElse(null);

            // uploaded directly in chatbot
            if (document != null) {

                documentService.validateAccess(document, user);

                if (document.getRagStatus() == null ||
                        document.getRagStatus() != RagStatus.DONE) {

                    if (document.getRagStatus() == RagStatus.FAILED) {

                        ragProcessingService.uploadToRagForNewUser(
                                documentUUID,
                                user.getId().toString()
                        );
                        throw new RuntimeException(
                                " There was a problem preparing this document.Please upload the file again or try later."
                        );
                    }

                    if (document.getRagStatus() == RagStatus.PENDING) {

                        throw new RuntimeException(
                                "Document is still being prepared, please try again in a moment."
                        );
                    }

                    ragProcessingService.uploadToRagForNewUser(
                            documentUUID,
                            user.getId().toString()
                    );
                    throw new RuntimeException(
                            "Document is not ready yet, please try again in a moment."
                    );
                }
            }

        } catch (IllegalArgumentException ignored) {

        }

        // =====================================
        // AI SESSION ID
        // =====================================
        String aiSessionId =
                user.getId() + "_" + session.getId();

        // =====================================
        // CALL RAG
        // =====================================
        System.out.println(
                "SESSION DOC = " +
                        session.getRagDocumentId()
        );

        System.out.println(
                "QUESTION = " +
                        request.getQuestion()
        );

        Map<String, Object> ragResponse =
                ragClient.askQuestion(

                        request.getQuestion(),

                        session.getRagDocumentId(),

                        aiSessionId,

                        user.getId().toString()
                );

        // =====================================
        // SAFE CHUNKS
        // =====================================
        Object chunksObj = ragResponse.get("chunks");

        List<String> chunks =
                (chunksObj instanceof List<?> list)

                        ? list.stream()
                        .map(String::valueOf)
                        .toList()

                        : List.of();

        // =====================================
        // SAVE MESSAGE
        // =====================================
        ChatMessage message = ChatMessage.builder()
                .sessionId(session.getId())
                .userId(user.getId().toString())
                .question((String) ragResponse.get("question"))
                .answer((String) ragResponse.get("answer"))
                .chunks(chunks)
                .timestamp(LocalDateTime.now())
                .build();

        messageRepo.save(message);

        session.setUpdatedAt(LocalDateTime.now());

        sessionRepo.save(session);

        // =====================================
        // RESPONSE
        // =====================================
        return AskChatResponse.builder()
                .sessionId(session.getId())
                .question(message.getQuestion())
                .answer(message.getAnswer())
                .build();
    }


    // =========================
    // DELETE SESSION
    // =========================
    public void deleteSession(
            String email,
            String sessionId
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        ChatSession session =
                sessionRepo.findByIdAndUserId(

                        sessionId,

                        user.getId().toString()

                ).orElseThrow(() ->
                        new RuntimeException("Session not found"));

        messageRepo.deleteBySessionId(session.getId());

        sessionRepo.delete(session);
    }

    // =========================
    // UPLOAD + ASK
    // =========================
    public AskChatResponse uploadAndAsk(

            String email,

            MultipartFile file,

            String question,

            String sessionId
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        ChatSession session;

        // =====================================
        // EXISTING SESSION
        // =====================================
        if (sessionId != null &&
                !sessionId.isBlank()) {

            session = sessionRepo.findByIdAndUserId(

                    sessionId,

                    user.getId().toString()

            ).orElseThrow(() ->
                    new RuntimeException("Session not found"));
        }

        // =====================================
        // NEW SESSION
        // =====================================
        else {

            session = ChatSession.builder()
                    .userId(user.getId().toString())
                    .title(file.getOriginalFilename())
                    .ragDocumentId(null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            sessionRepo.save(session);
        }

        // =====================================
        // GENERATE RAG DOC ID
        // =====================================
        String ragDocumentId =
                UUID.randomUUID().toString();

        // =====================================
        // UPLOAD TO RAG
        // =====================================
        ragClient.uploadDocument(

                file,

                ragDocumentId,

                user.getId().toString()
        );

        // =====================================
        // WAIT FOR PROCESSING
        // =====================================
        try {

            Thread.sleep(10000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        // =====================================
        // UPDATE SESSION CONTEXT
        // =====================================
        session.setRagDocumentId(ragDocumentId);

        session.setUpdatedAt(LocalDateTime.now());

        sessionRepo.save(session);

        // =====================================
        // ASK RAG
        // =====================================
        System.out.println(
                "RAG DOC ID = " +
                        session.getRagDocumentId()
        );

        Map<String, Object> ragResponse =
                ragClient.askQuestion(

                        question,

                        session.getRagDocumentId(),

                        user.getId().toString()
                                + "_"
                                + session.getId(),

                        user.getId().toString()
                );

        // =====================================
        // SAVE MESSAGE
        // =====================================
        ChatMessage message = ChatMessage.builder()

                .sessionId(session.getId())

                .userId(user.getId().toString())

                .question((String)
                        ragResponse.get("question"))

                .answer((String)
                        ragResponse.get("answer"))

                .timestamp(LocalDateTime.now())

                .build();

        messageRepo.save(message);

        // =====================================
        // RESPONSE
        // =====================================
        return AskChatResponse.builder()

                .sessionId(session.getId())

                .question(message.getQuestion())

                .answer(message.getAnswer())

                .build();
    }


    public List<ChatSessionSummary> getUserSessions(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatSession> sessions = sessionRepo
                .findByUserIdOrderByUpdatedAtDesc(user.getId().toString());

        return sessions.stream().map(session -> {


            List<ChatMessage> messages = messageRepo
                    .findBySessionIdOrderByTimestampAsc(session.getId());

            String lastMsg = messages.isEmpty() ? null :
                    messages.get(messages.size() - 1).getQuestion();

            return ChatSessionSummary.builder()
                    .sessionId(session.getId())
                    .title(session.getTitle())
                    .ragDocumentId(session.getRagDocumentId())
                    .updatedAt(session.getUpdatedAt())
                    .lastMessage(lastMsg)
                    .build();
        }).toList();
    }


    public List<ChatMessage> getSessionMessages(String email, String sessionId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        sessionRepo.findByIdAndUserId(sessionId, user.getId().toString())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        return messageRepo.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}