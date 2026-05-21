package com.example.EDIP.chatbot.repository;

import com.example.EDIP.chatbot.model.ChatSession;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository
        extends MongoRepository<ChatSession, String> {

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<ChatSession> findByIdAndUserId(String id, String userId);

    List<ChatSession> findByUserIdAndRagDocumentId(
            String userId,
            String ragDocumentId
    );

    @Query("{ 'userId': ?0, 'ragDocumentId': ?1 }")
    List<ChatSession> findByUserIdAndRagDocumentIdOrderByUpdatedAtDesc(
            String userId,
            String ragDocumentId,
            Sort sort
    );


}