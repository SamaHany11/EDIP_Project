package com.example.EDIP.document.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.document.model.sql.AiProcessingStatus;
import com.example.EDIP.document.model.sql.DocumentTracking;
import com.example.EDIP.document.repository.sql.DocumentRepository;
import com.example.EDIP.document.repository.sql.DocumentTrackingRepository;
import com.example.EDIP.document.dto.response.TrackingResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.example.EDIP.document.model.sql.DocumentStatus;
import java.time.format.DateTimeFormatter;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final DocumentTrackingRepository trackingRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Async
    public void track(UUID documentId, UUID changedBy) {

        DocumentTracking tracking = trackingRepository
                .findTopByDocumentIdOrderByChangedAtDesc(documentId);

        if (tracking == null) {
            tracking = new DocumentTracking();
            tracking.setDocumentId(documentId);
        }

        tracking.setChangedBy(changedBy);
        tracking.setChangedAt(LocalDateTime.now());

        trackingRepository.save(tracking);
    }

    public List<TrackingResponseDTO> getDocumentTracking(UUID documentId) {

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();

        validateAccess(document, currentUser);

        return trackingRepository
                .findByDocumentIdOrderByChangedAtAsc(documentId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
    private User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateAccess(Document document, User currentUser) {

        if ("ADMIN".equals(currentUser.getRole())) return;

        if (document.getSubmittedBy().equals(currentUser.getId())) return;

        if ("HEAD".equals(currentUser.getRole())) {
            if (document.getCurrentDepartmentId() != null &&
                    document.getCurrentDepartmentId().equals(currentUser.getDepartmentId())) {
                return;
            }
        }

        throw new RuntimeException("Access denied");
    }

    private TrackingResponseDTO toResponseDTO(DocumentTracking tracking) {

        Document document = documentRepository
                .findById(tracking.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        boolean hasError = document.getAiProcessingStatus() == AiProcessingStatus.AI_FAILED;

        return TrackingResponseDTO.builder()
                .status(formatStatus(document.getStatus()))
                .changedAt(tracking.getChangedAt())
                .hasError(hasError)
                .errorGuidance(hasError ? getErrorGuidance() : null)
                .build();
    }


    private String formatStatus(DocumentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case SUBMITTED -> "Submitted";
            case PENDING   -> "Pending";
            case APPROVED  -> "Pending";
            case COMPLETED -> "Completed";
        };
    }

    private String getErrorGuidance() {
        return """
               There was a problem processing your document. Please try:
               1. Resubmit the document
               2. Ensure file is not empty
               3. Check file format
               4. Contact support
               """;
    }
}