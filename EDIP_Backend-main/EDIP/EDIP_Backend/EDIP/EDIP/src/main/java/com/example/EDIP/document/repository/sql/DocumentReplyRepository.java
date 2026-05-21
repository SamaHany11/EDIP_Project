package com.example.EDIP.document.repository.sql;

import com.example.EDIP.document.model.sql.DocumentReply;
import com.example.EDIP.document.model.sql.ReplyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentReplyRepository extends JpaRepository<DocumentReply, UUID> {

    Optional<DocumentReply> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    Optional<DocumentReply> findById(UUID id);

    Optional<DocumentReply> findByDocumentIdAndReplyStatus(
            UUID documentId, ReplyStatus replyStatus);

    List<DocumentReply> findAllByDocumentIdAndReplyStatus(
            UUID documentId, ReplyStatus replyStatus);

    List<DocumentReply> findAllByDocumentIdOrderByApprovedAtDesc(UUID documentId);

    boolean existsByDocumentIdAndReplyStatus(
            UUID documentId, ReplyStatus replyStatus);

    Optional<DocumentReply> findTopByDocumentIdAndReplyStatusOrderByCreatedAtDesc(
            UUID documentId, ReplyStatus replyStatus);

    Optional<DocumentReply> findTopByDocumentIdAndPreparedByOrderByCreatedAtDesc(
            UUID documentId, UUID preparedBy);

    boolean existsByDocumentIdAndPreparedByAndReplyStatus(
            UUID documentId, UUID preparedBy, ReplyStatus replyStatus);


    List<DocumentReply> findAllByDocumentIdAndReplyStatusOrderByCreatedAtDesc(
            UUID documentId, ReplyStatus replyStatus);


    @Query("SELECT r FROM DocumentReply r WHERE r.documentId IN :documentIds " +
            "AND r.replyStatus = 'APPROVED' ORDER BY r.approvedAt DESC")
    List<DocumentReply> findApprovedRepliesByDocumentIds(
            @Param("documentIds") List<UUID> documentIds);


    List<DocumentReply> findAllByDocumentIdOrderByCreatedAtDesc(UUID documentId);



}
