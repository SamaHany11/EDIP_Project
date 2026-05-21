package com.example.EDIP.document.service;

import com.example.EDIP.Auth.model.Department;
import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.DepartmentRepository;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.chatbot.client.RagClient;
import com.example.EDIP.chatbot.model.RagStatus;
import com.example.EDIP.chatbot.service.RagProcessingService;
import com.example.EDIP.document.ai.AIProcessingClient;
import com.example.EDIP.document.dto.request.SubmitDocumentRequest;
import com.example.EDIP.document.dto.response.DocumentResponseDTO;
import com.example.EDIP.document.dto.response.ReplyResponseDTO;
import com.example.EDIP.document.exception.DocumentException;
import com.example.EDIP.document.model.mongo.ClassificationDocument;
import com.example.EDIP.document.model.mongo.OcrDocument;
import com.example.EDIP.document.model.sql.*;
import com.example.EDIP.document.repository.mongo.ClassificationDocumentRepository;
import com.example.EDIP.document.repository.mongo.OcrDocumentRepository;
import com.example.EDIP.document.repository.sql.AttachmentRepository;
import com.example.EDIP.document.repository.sql.DocumentReplyRepository;
import com.example.EDIP.document.repository.sql.DocumentRepository;
import com.example.EDIP.document.security.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.bson.types.ObjectId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.example.EDIP.document.dto.response.AttachmentDTO;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DepartmentRepository departmentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AIProcessingClient aiProcessingClient;
    private final GridFsTemplate gridFsTemplate;
    private final TrackingService trackingService;
    private final AutoLogService autoLogService;
    private final DocumentReplyRepository documentReplyRepository;
    private final AttachmentRepository attachmentRepository;
    private final CryptoService cryptoService;
    private final RagClient ragClient;
    private final OcrDocumentRepository ocrDocumentRepository;
    private final ClassificationDocumentRepository classificationDocumentRepository;
    private final RagProcessingService ragProcessingService;


    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public DocumentResponseDTO submitDocument(SubmitDocumentRequest request) {

        User currentUser = getCurrentUser();
        validateFiles(request.getFiles());
        MultipartFile mainFile = request.getFiles().get(0);

        String format = getFileFormat(mainFile);


        String mongoFileId = saveFileToMongo(mainFile, request.getConfidential());

        Document.DocumentBuilder builder = Document.builder()
                .documentFormat(format)
                .fileName(mainFile.getOriginalFilename())
                .status(DocumentStatus.SUBMITTED)
                .submittedBy(currentUser.getId())
                .confidential(request.getConfidential())
                .mongoFileId(mongoFileId);

        if (Boolean.FALSE.equals(request.getConfidential())
                && request.getTargetUserEmail() != null
                && !request.getTargetUserEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "targetUserEmail is only allowed for confidential documents"
            );
        }

        if (Boolean.TRUE.equals(request.getConfidential())) {

            if (request.getTargetUserEmail() == null || request.getTargetUserEmail().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Confidential documents require a target user email"
                );
            }

            User targetUser = userRepository.findByEmail(request.getTargetUserEmail())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No user found with email: " + request.getTargetUserEmail()
                    ));

            String targetRole = targetUser.getRole();
            boolean isHeadOrAdmin = "HEAD".equals(targetRole) || "ADMIN".equals(targetRole);

            if (!isHeadOrAdmin) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Confidential documents can only be sent to HEAD or ADMIN"
                );
            }
            if (!targetUser.isEnabled()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No user found with email: " + request.getTargetUserEmail()
                );
            }

            builder
                    .aiProcessingStatus(null)
                    .targetUserId(targetUser.getId())
                    .currentDepartmentId(null);

        } else {
            builder
                    .aiProcessingStatus(AiProcessingStatus.AI_PENDING)
                    .currentDepartmentId(null)
                    .targetUserId(null);
        }

        Document document = builder.build();


        Document savedDocument = documentRepository.save(document);
        savedDocument.setRagStatus(RagStatus.PENDING);





        ragProcessingService.uploadToRag(
                mainFile,
                savedDocument.getDocumentId(),
                currentUser.getId().toString()
        );


        for (int i = 1; i < request.getFiles().size(); i++) {
            saveAttachment(request.getFiles().get(i), savedDocument.getDocumentId());
        }

        if (!savedDocument.getConfidential()) {
            aiProcessingClient.processDocument(
                    mongoFileId,
                    savedDocument.getDocumentId().toString(),
                    currentUser.getId().toString()
            );
        }





        documentRepository.save(savedDocument);
        trackingService.track(
                savedDocument.getDocumentId(),
                currentUser.getId()
        );

        autoLogService.log(
                currentUser.getId(),
                savedDocument.getDocumentId(),
                "DOCUMENT_SUBMITTED",
                "Document submitted by: " + currentUser.getEmail(),
                null,
                DocumentStatus.SUBMITTED.name()
        );

        return toResponseDTO(savedDocument);
    }


    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return "";
        return filename.substring(lastDot + 1).toLowerCase();

    }


    private void validateFiles(List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            throw DocumentException.invalidFile("No files provided");
        }

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                throw DocumentException.invalidFile("Empty file found");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw DocumentException.invalidFile("File exceeds 10MB");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                throw DocumentException.invalidFile("Invalid file name");
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw DocumentException.invalidFile(
                        "Unsupported file type: " + contentType
                );
            }


            String ext = getExtension(fileName);

            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw DocumentException.invalidFile(
                        "Invalid file extension: " + ext
                );
            }
        }
    }




    public String saveFileToMongo(MultipartFile file, boolean isConfidential) {
        try {
            byte[] data;

            if (isConfidential) {
                data = cryptoService.encrypt(file.getBytes());
            } else {
                data = file.getBytes();
            }

            ObjectId fileId = gridFsTemplate.store(
                    new ByteArrayInputStream(data),
                    file.getOriginalFilename(),
                    file.getContentType()
            );

            return fileId.toString();

        } catch (IOException e) {
            throw DocumentException.invalidFile("Failed to save file");
        }
    }

    private String getFileFormat(MultipartFile file) {

        String fileName = file.getOriginalFilename();
        if (fileName == null) return "UNKNOWN";

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return "PDF";
        }

        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
            return "IMAGE";
        }

        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "WORD";
        }

        if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return "EXCEL";
        }

        return "UNKNOWN";
    }
    private String getMimeType(MultipartFile file) {
        return file.getContentType();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private DocumentResponseDTO toResponseDTO(Document document) {

        List<AttachmentDTO> attachments = attachmentRepository
                .findByDocumentId(document.getDocumentId())
                .stream()
                .map(a -> AttachmentDTO.builder()
                        .attachmentId(a.getAttachmentId())
                        .fileName(a.getFileName())
                        .fileType(a.getFileType())
                        .fileSize(a.getFileSize())
                        .build())
                .toList();

        String priority = classificationDocumentRepository
                .findBySqlDocumentId(document.getDocumentId().toString())
                .map(ClassificationDocument::getPriority)
                .orElse(null);

        return DocumentResponseDTO.builder()
                .documentId(document.getDocumentId())
                .fileName(document.getFileName())
                .documentFormat(formatDocumentFormat(document.getDocumentFormat()))
                .status(formatStatus(document.getStatus()))
                .priority(formatPriority(priority))
                .submittedDate(document.getSubmittedDate())
                .lastUpdated(document.getLastUpdated())
                .confidential(document.getConfidential())
                .attachments(attachments)
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

    private String formatDocumentFormat(String format) {
        if (format == null) return "—";
        return switch (format.toUpperCase()) {
            case "PDF"   -> "PDF Document";
            case "IMAGE" -> "Image";
            case "WORD"  -> "Word Document";
            case "EXCEL" -> "Excel Sheet";
            default      -> format;
        };
    }

    private String formatPriority(String priority) {
        if (priority == null) return "—";
        return switch (priority.toUpperCase()) {
            case "HIGH"   -> "High Priority";
            case "MEDIUM" -> "Medium Priority";
            case "LOW"    -> "Low Priority";
            default       -> priority;
        };
    }
    public Page<DocumentResponseDTO> getMyDocuments(String type, Pageable pageable) {

        User currentUser = getCurrentUser();

        Sort sort = Sort.by(
                Sort.Order.desc("submittedDate"),
                Sort.Order.desc("documentId")
        );

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        if (type == null || type.equals("all")) {
            return documentRepository
                    .findBySubmittedByAndIsDeletedFalse(
                            currentUser.getId(),
                            sortedPageable
                    )
                    .map(this::toResponseDTO);
        }

        if (type.equals("normal")) {
            return documentRepository
                    .findBySubmittedByAndConfidentialFalseAndIsDeletedFalse(
                            currentUser.getId(),
                            sortedPageable
                    )
                    .map(this::toResponseDTO);
        }

        if (type.equals("confidential")) {
            return documentRepository
                    .findBySubmittedByAndConfidentialTrueAndIsDeletedFalse(
                            currentUser.getId(),
                            sortedPageable
                    )
                    .map(this::toResponseDTO);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter");
    }

    public Page<DocumentResponseDTO> getDepartmentDocuments(Pageable pageable) {

        User currentUser = getCurrentUser();

        Sort sort = Sort.by(
                Sort.Order.desc("submittedDate"),
                Sort.Order.desc("documentId")
        );

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        String role = currentUser.getRole();

        if ("HEAD".equals(role)) {
            return documentRepository.findByCurrentDepartmentIdAndConfidentialFalseAndIsDeletedFalse(
                            currentUser.getDepartmentId(),
                            sortedPageable
                    )
                    .map(this::toResponseDTO);
        }
        if ("EMPLOYEE".equals(role)) {
            return documentRepository
                    .findByAssignedToAndIsDeletedFalse(
                            currentUser.getId(),
                            sortedPageable
                    )
                    .map(this::toResponseDTO);
        }

        throw DocumentException.accessDenied();
    }

    public ResponseEntity<byte[]> downloadFile(UUID documentId) {

        User currentUser = getCurrentUser();

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        validateAccess(document, currentUser);

        try {
            GridFSFile gridFSFile = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id")
                            .is(new ObjectId(document.getMongoFileId())))
            );

            if (gridFSFile == null) {
                throw DocumentException.invalidFile("File not found");
            }
            if (document.getMongoFileId() == null) {
                throw DocumentException.invalidFile("File reference missing");
            }

            GridFsResource resource = gridFsTemplate.getResource(gridFSFile);

            byte[] storedBytes = resource.getInputStream().readAllBytes();


            byte[] finalBytes;
            if (document.getConfidential()) {
                finalBytes = cryptoService.decrypt(storedBytes);
            } else {
                finalBytes = storedBytes;
            }

            String contentType =
                    gridFSFile.getMetadata() != null
                            ? gridFSFile.getMetadata().getString("_contentType")
                            : "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(finalBytes);

        } catch (IOException e) {
            throw DocumentException.invalidFile("Failed to download file");
        }
    }

    public ResponseEntity<byte[]> downloadAttachment(UUID attachmentId) {

        User currentUser = getCurrentUser();

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        if (attachment.getMongoFileId() == null) {
            throw DocumentException.invalidFile("Invalid file reference");
        }

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(attachment.getDocumentId())
                .orElseThrow(() -> DocumentException.notFound(attachment.getDocumentId()));

        validateAccess(document, currentUser);

        try {
            GridFSFile gridFSFile = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id")
                            .is(new ObjectId(attachment.getMongoFileId())))
            );
            if (gridFSFile == null) {
                throw DocumentException.invalidFile("File not found");
            }

            GridFsResource resource = gridFsTemplate.getResource(gridFSFile);


            byte[] storedBytes = resource.getInputStream().readAllBytes();

            // نخلي attachments encrypted زي ما كانت
            byte[] decryptedBytes = cryptoService.decrypt(storedBytes);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            attachment.getFileType() != null
                                    ? attachment.getFileType()
                                    : "application/octet-stream"
                    ))
                    .body(decryptedBytes);

        } catch (IOException e) {
            throw new RuntimeException("Download failed");
        }
    }

    public  void validateAccess(Document document, User currentUser) {

        String role = currentUser.getRole();


        if ("ADMIN".equals(role)) return;


        if (Boolean.TRUE.equals(document.getConfidential())) {

            if (document.getTargetUserId() != null &&
                    document.getTargetUserId().equals(currentUser.getId())
                    || document.getSubmittedBy().equals(currentUser.getId())
            ) {
                return;
            }

            throw DocumentException.accessDenied();
        }


        if ("EXTERNAL".equals(role)) {
            if (document.getSubmittedBy().equals(currentUser.getId())) return;
            throw DocumentException.accessDenied();
        }


        if ("HEAD".equals(role)) {
            if (document.getCurrentDepartmentId() != null &&
                    document.getCurrentDepartmentId().equals(currentUser.getDepartmentId())) {
                return;
            }
            throw DocumentException.accessDenied();
        }


        if ("EMPLOYEE".equals(role)) {
            if (document.getAssignedTo() != null &&
                    document.getAssignedTo().equals(currentUser.getId())) {
                return;
            }
            throw DocumentException.accessDenied();
        }

        throw DocumentException.accessDenied();
    }

    public ReplyResponseDTO getReply(UUID documentId) {
        User currentUser = getCurrentUser();

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        validateAccess(document, currentUser);

        DocumentReply reply = documentReplyRepository
                .findByDocumentIdAndReplyStatus(documentId, ReplyStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No approved reply yet"));

        return ReplyResponseDTO.builder()
                .replyText(reply.getReplyText())
                .fileName(reply.getFileName())
                .fileType(reply.getFileType())
                .repliedAt(reply.getApprovedAt())
                .hasFile(reply.getMongoFileId() != null)
                .build();
    }
    public List<ReplyResponseDTO> getReplies(UUID documentId) {

        User currentUser = getCurrentUser();

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        validateAccess(document, currentUser);

        List<DocumentReply> replies = documentReplyRepository
                .findAllByDocumentIdAndReplyStatus(
                        documentId,
                        ReplyStatus.APPROVED
                );

        return replies.stream()
                .map(reply -> ReplyResponseDTO.builder()
                        .replyId(reply.getReplyId())
                        .replyText(reply.getReplyText())
                        .fileName(reply.getFileName())
                        .fileType(reply.getFileType())
                        .repliedAt(reply.getApprovedAt())
                        .hasFile(reply.getMongoFileId() != null)
                        .build())
                .toList();
    }

    public ResponseEntity<byte[]> downloadReplyFile(UUID documentId) {
        User currentUser = getCurrentUser();

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        validateAccess(document, currentUser);

        DocumentReply reply = documentReplyRepository
                .findByDocumentIdAndReplyStatus(documentId, ReplyStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No approved reply yet"));

        if (reply.getMongoFileId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No file attached to this reply");
        }

        GridFSFile gridFSFile = gridFsTemplate.findOne(
                new Query(Criteria.where("_id")
                        .is(new ObjectId(reply.getMongoFileId())))
        );

        if (gridFSFile == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "File not found");
        }

        try {
            GridFsResource resource = gridFsTemplate.getResource(gridFSFile);
            byte[] bytes = resource.getInputStream().readAllBytes();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(reply.getFileType()))
                    .header("Content-Disposition",
                            "attachment; filename=\"" + reply.getFileName() + "\"")
                    .body(bytes);

        } catch (IOException e) {
            throw DocumentException.invalidFile("Failed to download reply file");
        }
    }

    private Attachment saveAttachment(MultipartFile file, UUID documentId) {

        String mongoId = saveFileToMongo(file, true);

        Attachment attachment = new Attachment();
        attachment.setDocumentId(documentId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(getMimeType(file));
        attachment.setFileSize(file.getSize());
        attachment.setMongoFileId(mongoId);

        return attachmentRepository.save(attachment);
    }

    public void resubmitDocument(UUID documentId) {

        User currentUser = getCurrentUser();

        Document doc = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        // ─────────────────────────────
        // 1. Authorization check
        // ─────────────────────────────
        if (!doc.getSubmittedBy().equals(currentUser.getId())) {
            throw DocumentException.accessDenied();
        }

        // ─────────────────────────────
        // 2. Business rule: confidential not allowed
        // ─────────────────────────────
        if (Boolean.TRUE.equals(doc.getConfidential())) {
            throw new RuntimeException("Confidential documents cannot be resubmitted");
        }

        // ─────────────────────────────
        // 3. Prevent duplicate processing
        // ─────────────────────────────
        if (doc.getAiProcessingStatus() == AiProcessingStatus.AI_PENDING) {
            throw new RuntimeException("Document is already being processed");
        }

        if (doc.getAiProcessingStatus() == AiProcessingStatus.AI_DONE) {
            throw new RuntimeException("Document already processed successfully");
        }

        // ─────────────────────────────
        // 4. Reset AI state
        // ─────────────────────────────
        doc.setAiProcessingStatus(AiProcessingStatus.AI_PENDING);
        doc.setAiRetryCount(0);

        documentRepository.save(doc);

        // ─────────────────────────────
        // 5. Trigger AI again
        // ─────────────────────────────
        aiProcessingClient.processDocument(
                doc.getMongoFileId(),
                doc.getDocumentId().toString(),
                currentUser.getId().toString()
        );

        documentRepository.save(doc);

        trackingService.track(
                doc.getDocumentId(),
                currentUser.getId()
        );
    }

    public String getDocumentSummary(UUID documentId) {

        User currentUser = getCurrentUser();

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        validateAccess(document, currentUser);
        validateDocumentReady(document);

        String role = currentUser.getRole();

        if (!role.equals("ADMIN") &&
                !role.equals("HEAD") &&
                !role.equals("EMPLOYEE")) {
            throw DocumentException.accessDenied();
        }



        OcrDocument ocr = ocrDocumentRepository
                .findBySqlDocumentId(documentId.toString())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Document summary is not ready yet"
                        )
                );

        return ocr.getSummary();
    }

    private void validateDocumentReady(Document document) {
        if (document.getConfidential() != null && document.getConfidential()) {
            return;
        }

        if (document.getAiProcessingStatus() == null ||
                document.getAiProcessingStatus() != AiProcessingStatus.AI_DONE) {

            AiProcessingStatus status = document.getAiProcessingStatus();
            String message;

            if (status == AiProcessingStatus.AI_PENDING) {
                message = "Document is still being processed, please try again later";
            } else if (status == AiProcessingStatus.AI_FAILED) {
                message = "Document processing failed, please re-upload the document";
            } else {
                message = "Document has not been processed yet";
            }

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        if (document.getMongoFileId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document data is missing"
            );
        }
    }

    public Page<DocumentResponseDTO> getConfidentialDocuments(Pageable pageable) {

        User currentUser = getCurrentUser();

        log.info("Auth name = {}",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("submittedDate").descending()
        );

        return documentRepository
                .findConfidentialDocsForUser(currentUser.getId(), sortedPageable)
                .map(this::toResponseDTO);
    }


    public List<ReplyResponseDTO> getMyApprovedReplies() {

        User currentUser = getCurrentUser();


        List<UUID> myDocumentIds = documentRepository
                .findBySubmittedByAndIsDeletedFalse(currentUser.getId(), Pageable.unpaged())
                .stream()
                .filter(doc -> doc.getStatus() == DocumentStatus.COMPLETED)
                .map(Document::getDocumentId)
                .toList();

        if (myDocumentIds.isEmpty()) return List.of();


        return documentReplyRepository
                .findApprovedRepliesByDocumentIds(myDocumentIds)
                .stream()
                .map(reply -> {
                    User creator = userRepository
                            .findById(reply.getPreparedBy())
                            .orElse(null);

                    return ReplyResponseDTO.builder()
                            .replyId(reply.getReplyId())
                            .documentId(reply.getDocumentId())
                            .replyText(reply.getReplyText())
                            .fileName(reply.getFileName())
                            .fileType(reply.getFileType())
                            .hasFile(reply.getMongoFileId() != null)
                            .replyStatus(reply.getReplyStatus().name())
                            .createdByName(creator != null
                                    ? creator.getUsername() : "Unknown")
                            .createdAt(reply.getCreatedAt())
                            .repliedAt(reply.getApprovedAt())
                            .build();
                })
                .toList();
    }
    public ResponseEntity<byte[]> downloadReplyFileById(UUID replyId) {
        User currentUser = getCurrentUser();

        DocumentReply reply = documentReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Reply not found"));

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(reply.getDocumentId())
                .orElseThrow(() -> DocumentException.notFound(reply.getDocumentId()));
        validateReplyAccess(reply, currentUser);
        validateAccess(document, currentUser);

        if (reply.getMongoFileId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No file attached to this reply");
        }

        try {
            GridFSFile gridFSFile = gridFsTemplate.findOne(
                    new Query(Criteria.where("_id")
                            .is(new ObjectId(reply.getMongoFileId())))
            );

            if (gridFSFile == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "File not found");
            }

            GridFsResource resource = gridFsTemplate.getResource(gridFSFile);
            byte[] bytes = resource.getInputStream().readAllBytes();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            reply.getFileType() != null
                                    ? reply.getFileType()
                                    : "application/octet-stream"))
                    .header("Content-Disposition",
                            "attachment; filename=\"" + reply.getFileName() + "\"")
                    .body(bytes);

        } catch (IOException e) {
            throw DocumentException.invalidFile("Failed to download reply file");
        }
    }
    public Map<String, String> getMyDepartment() {
        User currentUser = getCurrentUser();

        String departmentName = departmentRepository
                .findById(currentUser.getDepartmentId())
                .map(Department::getDepartmentName)
                .orElse("Unknown");

        Map<String, String> response = new HashMap<>();
        response.put("departmentName", departmentName);
        return response;
    }
    private void validateReplyAccess(DocumentReply reply, User currentUser) {

        String role = currentUser.getRole();


        if ("ADMIN".equals(role) || "HEAD".equals(role)) {
            return;
        }

        if ("EMPLOYEE".equals(role)) {

            if (reply.getPreparedBy().equals(currentUser.getId())) {
                return;
            }

            throw DocumentException.accessDenied();
        }

        // External
        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(reply.getDocumentId())
                .orElseThrow();

        if (document.getSubmittedBy().equals(currentUser.getId())) {
            return;
        }

        throw DocumentException.accessDenied();
    }
    public List<ReplyResponseDTO> getAllReplies(UUID documentId) {
        User currentUser = getCurrentUser();
        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));
        validateAccess(document, currentUser);

        return documentReplyRepository
                .findAllByDocumentIdOrderByCreatedAtDesc(documentId)
                .stream()
                .map(reply -> {
                    User creator = userRepository.findById(reply.getPreparedBy()).orElse(null);
                    return ReplyResponseDTO.builder()
                            .replyId(reply.getReplyId())
                            .documentId(documentId)
                            .replyText(reply.getReplyText())
                            .fileName(reply.getFileName())
                            .fileType(reply.getFileType())
                            .hasFile(reply.getMongoFileId() != null)
                            .replyStatus(reply.getReplyStatus().name())
                            .createdByName(creator != null ? creator.getUsername() : "Unknown")
                            .createdByEmail(creator != null ? creator.getEmail() : null)
                            .createdAt(reply.getCreatedAt())
                            .repliedAt(reply.getApprovedAt())
                            .build();
                })
                .toList();
    }
    public ReplyResponseDTO getLatestReply(UUID documentId) {

        DocumentReply reply = documentReplyRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No replies found"
                ));

        User creator = userRepository
                .findById(reply.getPreparedBy())
                .orElse(null);

        User rejectedBy = null;

        if (reply.getRejectedBy() != null) {
            rejectedBy = userRepository
                    .findById(reply.getRejectedBy())
                    .orElse(null);
        }

        return ReplyResponseDTO.builder()
                .replyId(reply.getReplyId())
                .documentId(reply.getDocumentId())
                .replyText(reply.getReplyText())
                .fileName(reply.getFileName())
                .fileType(reply.getFileType())
                .hasFile(reply.getMongoFileId() != null)

                .replyStatus(reply.getReplyStatus().name())

                .rejectionReason(reply.getRejectionReason())

                .createdByName(
                        creator != null
                                ? creator.getUsername()
                                : null
                )

                .createdByEmail(
                        creator != null
                                ? creator.getEmail()
                                : null
                )

                .rejectedByName(
                        rejectedBy != null
                                ? rejectedBy.getUsername()
                                : null
                )

                .rejectedByEmail(
                        rejectedBy != null
                                ? rejectedBy.getEmail()
                                : null
                )

                .rejectedAt(reply.getRejectedAt())

                .createdAt(reply.getCreatedAt())

                .repliedAt(reply.getApprovedAt())

                .build();
    }
}