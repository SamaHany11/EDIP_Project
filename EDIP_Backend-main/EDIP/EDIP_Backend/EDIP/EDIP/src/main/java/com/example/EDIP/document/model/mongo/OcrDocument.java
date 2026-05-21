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
@Document(collection = "ocr_documents")
public class OcrDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sqlDocumentId;

    private String rowText;
    private String summary;
    private String receivedAt;

    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}