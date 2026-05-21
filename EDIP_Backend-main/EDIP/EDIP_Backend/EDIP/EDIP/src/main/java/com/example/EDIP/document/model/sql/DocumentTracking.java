package com.example.EDIP.document.model.sql;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Document_Tracking", indexes = {
        @Index(name = "IDX_Tracking_Document", columnList = "document_id"),
        @Index(name = "IDX_Tracking_Date",     columnList = "changed_at")
})
public class DocumentTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tracking_id")
    private UUID trackingId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}