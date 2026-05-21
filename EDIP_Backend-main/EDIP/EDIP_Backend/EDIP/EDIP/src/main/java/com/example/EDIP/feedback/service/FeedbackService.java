package com.example.EDIP.feedback.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.document.model.sql.DocumentStatus;
import com.example.EDIP.document.repository.sql.DocumentRepository;
import com.example.EDIP.document.service.NotificationService;
import com.example.EDIP.feedback.dto.FeedbackResponseDTO;
import com.example.EDIP.feedback.dto.SubmitFeedbackRequest;
import com.example.EDIP.feedback.model.Feedback;
import com.example.EDIP.feedback.model.FeedbackStatus;
import com.example.EDIP.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final NotificationService notificationService;

    public String submitFeedback(UUID documentId, SubmitFeedbackRequest request) {

        User user = getCurrentUser();

        if (!"EXTERNAL".equals(user.getRole())) {
            throw new RuntimeException("Only external users can submit feedback");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getSubmittedBy().equals(user.getId())) {
            throw new RuntimeException("You can only feedback your own documents");
        }

        if (document.getStatus() != DocumentStatus.COMPLETED) {
            throw new RuntimeException("Feedback allowed only for completed documents");
        }

        if (feedbackRepository.existsByDocumentId(documentId)) {
            throw new RuntimeException("Feedback already submitted for this document");
        }

        Feedback feedback = Feedback.builder()
                .userId(user.getId())
                .documentId(documentId)
                .rating(request.getRating())
                .feedbackContent(request.getFeedbackContent())
                .status(FeedbackStatus.NEW)
                .submittedAt(LocalDateTime.now())
                .build();

        feedbackRepository.save(feedback);
        notificationService.notifyHead(
                document.getCurrentDepartmentId(),
                documentId,
                "FEEDBACK",
                "New feedback submitted by " + user.getUsername(),
                "MEDIUM"
        );
        return "Feedback submitted successfully";
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public Page<FeedbackResponseDTO> getAllFeedbacks(
            FeedbackStatus status,
            Pageable pageable
    ) {
        User head = getCurrentUser();

        if (!"HEAD".equals(head.getRole())) {
            throw new RuntimeException("Access denied");
        }



        Page<Feedback> feedbacks;

        if (status != null) {
            feedbacks = feedbackRepository.findByDepartmentIdAndStatus(
                    head.getDepartmentId(),
                    status,
                    pageable
            );
        } else {
            feedbacks = feedbackRepository.findByDepartmentId(
                    head.getDepartmentId(),
                    pageable
            );
        }

        return feedbacks.map(this::toDTO);
    }
    public String markAsReviewed(UUID feedbackId) {

        User head = getCurrentUser();

        if (!"HEAD".equals(head.getRole())) {
            throw new RuntimeException("Access denied");
        }

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        Document document = documentRepository.findById(feedback.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw new RuntimeException("Access denied");
        }

        feedback.setStatus(FeedbackStatus.REVIEWED);
        feedbackRepository.save(feedback);

        return "Feedback marked as reviewed";
    }
    private FeedbackResponseDTO toDTO(Feedback feedback) {

        Document document = documentRepository.findById(feedback.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return FeedbackResponseDTO.builder()
                .feedbackId(feedback.getFeedbackId())
                .documentId(feedback.getDocumentId())

                .departmentId(document.getCurrentDepartmentId())
                .departmentName(document.getDepartmentName())

                .rating(feedback.getRating())
                .feedbackContent(feedback.getFeedbackContent())
                .status(feedback.getStatus())
                .submittedAt(feedback.getSubmittedAt())
                .build();
    }
}
