package com.example.EDIP.feedback.controller;

import com.example.EDIP.feedback.dto.FeedbackResponseDTO;
import com.example.EDIP.feedback.dto.SubmitFeedbackRequest;
import com.example.EDIP.feedback.model.FeedbackStatus;
import com.example.EDIP.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    @PreAuthorize("hasRole('EXTERNAL')")
    @PostMapping("/{documentId}")
    public String submitFeedback(
            @PathVariable UUID documentId,
            @Valid @RequestBody SubmitFeedbackRequest request
    ) {
        return feedbackService.submitFeedback(documentId, request);
    }
    @PreAuthorize("hasRole('HEAD')")
    @GetMapping
    public Page<FeedbackResponseDTO> getAllFeedbacks(
            @RequestParam(required = false) FeedbackStatus status,
            Pageable pageable
    ) {
        return feedbackService.getAllFeedbacks(status, pageable);
    }
    @PreAuthorize("hasRole('HEAD')")
    @PutMapping("/{feedbackId}/review")
    public String markAsReviewed(@PathVariable UUID feedbackId) {
        return feedbackService.markAsReviewed(feedbackId);
    }
}