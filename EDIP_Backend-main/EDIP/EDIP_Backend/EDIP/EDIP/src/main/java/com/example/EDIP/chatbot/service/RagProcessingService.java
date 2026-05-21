package com.example.EDIP.chatbot.service;

import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.chatbot.model.RagStatus;
import com.example.EDIP.document.repository.sql.DocumentRepository;
import com.example.EDIP.chatbot.client.RagClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.bson.types.ObjectId;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagProcessingService {

    private final RagClient ragClient;
    private final DocumentRepository documentRepository;
    private final GridFsTemplate gridFsTemplate;

    @Async
    public void uploadToRag(
            MultipartFile file,
            UUID documentId,
            String userId
    ) {

        try {

            ragClient.uploadDocument(
                    file,
                    documentId.toString(),
                    userId
            );

            updateRagStatus(documentId, RagStatus.DONE);

        }catch (Exception e) {

            log.error("FULL RAG ERROR", e);

            updateRagStatus(documentId, RagStatus.FAILED);

        }
    }

    private void updateRagStatus(UUID documentId, RagStatus status) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setRagStatus(status);

        documentRepository.save(document);
    }


    @Async
    public void uploadToRagForNewUser(
            UUID documentId,
            String newUserId
    ) {
        try {

            Document document = documentRepository
                    .findByDocumentIdAndIsDeletedFalse(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));


            String mongoFileId = document.getMongoFileId();
            if (mongoFileId == null || mongoFileId.isBlank()) {
                throw new RuntimeException("Document file not found in MongoDB");
            }


            GridFSFile gridFSFile = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id")
                            .is(new ObjectId(mongoFileId)))
            );

            if (gridFSFile == null) {
                throw new RuntimeException("GridFS file not found");
            }


            byte[] fileContent = gridFsTemplate.getResource(gridFSFile)
                    .getInputStream()
                    .readAllBytes();

            String fileName = gridFSFile.getFilename();
            String contentType = gridFSFile.getMetadata() != null
                    ? gridFSFile.getMetadata().get("contentType", String.class)
                    : "application/octet-stream";


            ragClient.uploadDocumentFromInputStream(
                    new ByteArrayInputStream(fileContent),
                    fileName,
                    contentType,
                    documentId.toString(),
                    newUserId
            );

            log.info("Document {} uploaded to RAG for new user: {}", documentId, newUserId);

        } catch (Exception e) {
            log.error("Failed to upload document {} to RAG for user {}", documentId, newUserId, e);
        }
    }
}