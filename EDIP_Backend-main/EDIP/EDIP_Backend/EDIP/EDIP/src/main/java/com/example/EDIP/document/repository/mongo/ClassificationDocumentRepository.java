package com.example.EDIP.document.repository.mongo;

import com.example.EDIP.document.model.mongo.ClassificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClassificationDocumentRepository
        extends MongoRepository<ClassificationDocument, String> {

    Optional<ClassificationDocument> findBySqlDocumentId(String sqlDocumentId);
}