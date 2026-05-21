package com.example.EDIP.document.repository.mongo;

import com.example.EDIP.document.model.mongo.OcrDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OcrDocumentRepository
        extends MongoRepository<OcrDocument, String> {

    Optional<OcrDocument> findBySqlDocumentId(String sqlDocumentId);
}