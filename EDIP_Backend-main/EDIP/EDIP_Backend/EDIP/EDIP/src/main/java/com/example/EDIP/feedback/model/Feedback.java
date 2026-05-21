package com.example.EDIP.feedback.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue
    @Column(name = "feedback_id")
    private UUID feedbackId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "feedback_content", nullable = false, length = 2000)
    private String feedbackContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FeedbackStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}