package com.example.EDIP.document.ai;

import com.example.EDIP.chatbot.service.RagProcessingService;
import com.example.EDIP.document.model.mongo.ClassificationDocument;
import com.example.EDIP.document.model.mongo.OcrDocument;
import com.example.EDIP.document.model.sql.AiProcessingStatus;
import com.example.EDIP.document.repository.mongo.ClassificationDocumentRepository;
import com.example.EDIP.document.repository.mongo.OcrDocumentRepository;
import com.example.EDIP.document.repository.sql.DocumentRepository;
import com.example.EDIP.document.routing.RoutingRulesService;
import com.example.EDIP.document.service.NotificationService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIProcessingClient {

    private final OcrDocumentRepository ocrDocumentRepository;
    private final ClassificationDocumentRepository classificationRepository;
    private final DocumentRepository documentRepository;
    private final RoutingRulesService routingRulesService;
    private final WebClient webClient;
    private final GridFsTemplate gridFsTemplate;
    private final NotificationService notificationService;
    private final RagProcessingService ragProcessingService;
    private final com.example.EDIP.Auth.repository.UserRepository userRepository;
    private final java.util.Set<String> processing = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Value("${ai.api.key}")
    private String apiKey;



    // ─────────────────────────────────────────────
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void processAI(String mongoFileId, String sqlDocumentId, String userId) {

        if (!processing.add(sqlDocumentId)) {
            log.warn("AI already processing {}", sqlDocumentId);
            return;
        }

        try {

            log.info("AI processing document {}", sqlDocumentId);

            byte[] fileBytes = getFileFromMongo(mongoFileId);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", fileBytes).filename("document.pdf");

            AIApiResponseWrapper response = webClient.post()
                    .uri("")
                    .header("X-Groq-Api-Key", apiKey)
                    .header("x-user-id", userId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(AIApiResponseWrapper.class)
                    .block();

            if (response == null || response.getData() == null) {
                throw new RuntimeException("AI returned null response");
            }

            saveOcrData(sqlDocumentId, response);
            saveClassificationData(sqlDocumentId, response);

            updateDocument(sqlDocumentId, response, AiProcessingStatus.AI_DONE);

            log.info("AI SUCCESS for document {}", sqlDocumentId);

        } finally {
            processing.remove(sqlDocumentId);
        }
    }
    @Recover
    public void recover(
            Exception e,
            String mongoFileId,
            String sqlDocumentId,
            String userId
    ) {

        log.error("AI FAILED permanently for {}", sqlDocumentId);

        markAsFailed(sqlDocumentId);
    }
    @Async
    public void processDocument(String mongoFileId, String sqlDocumentId, String userId)  {

        documentRepository.findByDocumentIdAndIsDeletedFalse(UUID.fromString(sqlDocumentId))
                .ifPresent(doc -> {

                    if (doc.getAiProcessingStatus() == AiProcessingStatus.AI_DONE) {
                        log.info("Already processed {}", sqlDocumentId);
                        return;
                    }

                    if (doc.getAiProcessingStatus() == AiProcessingStatus.AI_PENDING) {
                        log.info("Already in progress {}", sqlDocumentId);
                        return;
                    }

                    doc.setAiProcessingStatus(AiProcessingStatus.AI_PENDING);
                    doc.setAiRetryCount(0);
                    documentRepository.save(doc);
                });

        processAI(mongoFileId, sqlDocumentId, userId);
    }


    private void markAsFailed(String sqlDocumentId) {

        documentRepository.findByDocumentIdAndIsDeletedFalse(UUID.fromString(sqlDocumentId))
                .ifPresent(doc -> {

                    doc.setAiProcessingStatus(AiProcessingStatus.AI_FAILED);
                    documentRepository.save(doc);

                    notificationService.notify(
                            doc.getSubmittedBy(),
                            doc.getDocumentId(),
                            "AI_FAILED_FINAL",
                            "Document failed. Click to resubmit.",
                            "HIGH"
                    );
                });
    }

    // ─────────────────────────────────────────────


    // ─────────────────────────────────────────────
    private byte[] getFileFromMongo(String mongoFileId) {
        try {
            GridFSFile file = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id").is(new ObjectId(mongoFileId)))
            );

            GridFsResource resource = gridFsTemplate.getResource(file);
            return resource.getInputStream().readAllBytes();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load file from Mongo");
        }
    }

    // ─────────────────────────────────────────────
    private void saveOcrData(String sqlDocumentId, AIApiResponseWrapper response) {

        OcrDocument ocr = OcrDocument.builder()
                .sqlDocumentId(sqlDocumentId)
                .rowText(response.getData().getRawText())
                .summary(response.getData().getSummary())
                .build();

        ocrDocumentRepository.save(ocr);
    }

    // ─────────────────────────────────────────────
    private void saveClassificationData(String sqlDocumentId, AIApiResponseWrapper response) {

        ClassificationDocument c = ClassificationDocument.builder()
                .sqlDocumentId(sqlDocumentId)
                .department(response.getData().getDepartment())
                .priority(response.getData().getPriority())
                .route(response.getData().isRoute())
                .build();

        classificationRepository.save(c);
    }

    // ─────────────────────────────────────────────
    private void updateDocument(
            String sqlDocumentId,
            AIApiResponseWrapper response,
            AiProcessingStatus status
    ) {

        documentRepository.findByDocumentIdAndIsDeletedFalse(UUID.fromString(sqlDocumentId))
                .ifPresent(doc -> {

                    String departmentNameFromAI = response.getData().getDepartment();


                    if (departmentNameFromAI == null || departmentNameFromAI.trim().isEmpty()) {
                        log.warn("AI failed to identify department. currentDepartmentId will be NULL");
                        doc.setCurrentDepartmentId(null);
                        doc.setAiProcessingStatus(status);
                        documentRepository.save(doc);
                        return;
                    }


                    UUID departmentId = routingRulesService.findDepartmentByName(departmentNameFromAI);


                    if (departmentId != null) {
                        doc.setDepartmentName(departmentNameFromAI);
                        doc.setCurrentDepartmentId(departmentId);
                    } else {
                        doc.setCurrentDepartmentId(null);
                    }

                    doc.setAiProcessingStatus(status);
                    documentRepository.save(doc);


                    if (departmentId != null) {
                        userRepository.findHeadByDepartmentId(departmentId)
                                .ifPresent(head ->
                                        ragProcessingService.uploadToRagForNewUser(
                                                doc.getDocumentId(),
                                                head.getId().toString()
                                        )
                                );
                    }

                    notificationService.notifyHead(
                            departmentId,
                            doc.getDocumentId(),
                            "NEW_DOCUMENT",
                            "New document assigned to your department",
                            "HIGH"
                    );
                });
    }

    // ─────────────────────────────────────────────
    static class AIApiResponseWrapper {

        private boolean success;
        private Data data;

        public Data getData() { return data; }
        public void setData(Data data) { this.data = data; }

        static class Data {

            @JsonProperty("raw_text") private String rawText;
            @JsonProperty("summary") private String summary;
            @JsonProperty("department") private String department;
            @JsonProperty("priority") private String priority;
            @JsonProperty("route") private boolean route;

            public String getRawText() { return rawText; }
            public void setRawText(String v) { this.rawText = v; }

            public String getSummary() { return summary; }
            public void setSummary(String v) { this.summary = v; }

            public String getDepartment() { return department; }
            public void setDepartment(String v) { this.department = v; }

            public String getPriority() { return priority; }
            public void setPriority(String v) { this.priority = v; }

            public boolean isRoute() { return route; }
            public void setRoute(boolean v) { this.route = v; }
        }
    }
}
