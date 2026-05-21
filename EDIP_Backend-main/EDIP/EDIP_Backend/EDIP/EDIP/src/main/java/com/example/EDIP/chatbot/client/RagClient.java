package com.example.EDIP.chatbot.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RagClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${rag.base-url}")
    private String ragBaseUrl;

    @Value("${rag.groq-key}")
    private String groqKey;

    // =========================
    // UPLOAD DOCUMENT
    // =========================
    public boolean uploadDocument(
            MultipartFile file,
            String documentId,
            String userId
    ) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        headers.set("x-groq-key", groqKey);
        headers.set("x-user-id", userId);

        ByteArrayResource resource;

        try {

            resource = new ByteArrayResource(file.getBytes()) {

                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add("file", resource);
        body.add("document_id", documentId);

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        ragBaseUrl + "/upload-document",
                        request,
                        String.class
                );

        System.out.println("UPLOAD STATUS = " + response.getStatusCode());
        System.out.println("UPLOAD BODY = " + response.getBody());

        if (!response.getStatusCode().is2xxSuccessful()) {

            throw new RuntimeException(
                    "Upload failed to RAG service"
            );
        }

        return response.getStatusCode().is2xxSuccessful();
    }

    // =========================
    // ASK QUESTION
    // =========================
    public Map askQuestion(
            String question,
            String documentId,
            String sessionId,
            String userId
    ) {

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("x-groq-key", groqKey);
        headers.set("x-user-id", userId);

        HttpEntity<?> request = new HttpEntity<>(

                Map.of(
                        "question", question,
                        "document_id", documentId
                ),

                headers
        );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        ragBaseUrl + "/ask",
                        request,
                        Map.class
                );

        System.out.println("ASK RESPONSE = " + response.getBody());

        return response.getBody();
    }

    public void uploadDocumentFromInputStream(
            InputStream fileInputStream,
            String fileName,
            String contentType,
            String documentId,
            String userId
    ) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("x-groq-key", groqKey);
            headers.set("x-user-id", userId);

            byte[] fileBytes = fileInputStream.readAllBytes();

            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            body.add("document_id", documentId);

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    ragBaseUrl + "/upload-document",
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Upload to RAG failed");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document to RAG", e);
        }
    }
}