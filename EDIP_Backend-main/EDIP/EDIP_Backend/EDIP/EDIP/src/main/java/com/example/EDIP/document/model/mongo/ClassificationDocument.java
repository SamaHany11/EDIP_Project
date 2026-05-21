package com.example.EDIP.document.model.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "classification_documents")
public class ClassificationDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sqlDocumentId;

    private String docInfo;
    private String entity;
    private String department;
    private String name;
    private String priority;

    private boolean route;

    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();

}