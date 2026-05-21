package com.example.EDIP.chatbot.repository;

import com.example.EDIP.chatbot.model.ChatMessage;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;


public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);


    void deleteBySessionId(String sessionId);



}