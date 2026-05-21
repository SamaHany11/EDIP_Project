package com.example.EDIP.document.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.document.dto.request.AssignToEmployeeRequest;
import com.example.EDIP.document.dto.request.CompleteDocumentRequest;
import com.example.EDIP.document.dto.request.ForwardToDepartmentRequest;
import com.example.EDIP.document.dto.request.ReplyRequest;
import com.example.EDIP.document.dto.response.DocumentResponseDTO;
import com.example.EDIP.document.dto.response.ReplyResponseDTO;
import com.example.EDIP.document.exception.DocumentException;
import com.example.EDIP.document.model.sql.*;
import com.example.EDIP.document.repository.sql.DocumentReplyRepository;
import com.example.EDIP.document.repository.sql.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.example.EDIP.chatbot.service.RagProcessingService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeadService {

    private final DocumentRepository documentRepository;
    private final DocumentReplyRepository documentReplyRepository;
    private final UserRepository userRepository;
    private final AutoLogService autoLogService;
    private final NotificationService notificationService;
    private final GridFsTemplate gridFsTemplate;
    private final RagProcessingService ragProcessingService;


    @Transactional
    public DocumentResponseDTO assignToEmployee(
            UUID documentId, AssignToEmployeeRequest request) {

        User head = getCurrentUser();
        validateHeadRole(head);

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        if (document.getCurrentDepartmentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document is not assigned to any department yet");
        }

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw DocumentException.accessDenied();
        }
        if (Boolean.TRUE.equals(document.getConfidential())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confidential documents cannot be assigned to employees");
        }

        User employee = userRepository.findByEmail(request.getEmployeeEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));

        if (!employee.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Account is disabled");
        }
        if (!"EMPLOYEE".equals(employee.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only employees can be assigned to this document");
        }
        if (employee.getDepartmentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Employee department is missing");
        }
        if (!employee.getDepartmentId().equals(head.getDepartmentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only employees in your department can be assigned");
        }
        if (document.getAssignedTo() != null &&
                document.getAssignedTo().equals(employee.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Document is already assigned to this employee");
        }

        document.setAssignedTo(employee.getId());
        document.setStatus(DocumentStatus.PENDING);
        documentRepository.save(document);


        ragProcessingService.uploadToRagForNewUser(
                documentId,
                employee.getId().toString()
        );

        autoLogService.log(head.getId(), documentId, "ASSIGNED_TO_EMPLOYEE",
                "Assigned to: " + employee.getEmail(),
                head.getDepartmentId(), DocumentStatus.PENDING.name());

        notificationService.notify(employee.getId(), documentId,
                "DOCUMENT_ASSIGNED",
                "You have been assigned a new document to review", "HIGH");

        return toResponseDTO(document);
    }


    @Transactional
    public void forwardToDepartment(
            UUID documentId, ForwardToDepartmentRequest request) {

        User head = getCurrentUser();
        validateHeadRole(head);

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        if (document.getCurrentDepartmentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document is not assigned to any department yet");
        }

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw DocumentException.accessDenied();
        }
        if (Boolean.TRUE.equals(document.getConfidential())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confidential documents cannot be forwarded");
        }

        validateDocumentReady(document);

        if (document.getStatus() == DocumentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot forward a completed document.");
        }

        User target = userRepository.findByEmail(request.getDepartmentEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No department found"));

        if (!"HEAD".equals(target.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Must be HEAD");
        }
        if (target.getDepartmentId().equals(head.getDepartmentId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Same department not allowed");
        }

        if (request.getFinalSignedFile() != null &&
                !request.getFinalSignedFile().isEmpty()) {

            DocumentReply latestReply = documentReplyRepository
                    .findTopByDocumentIdAndReplyStatusOrderByCreatedAtDesc(documentId, ReplyStatus.APPROVED)
                    .orElse(null);
            try {
                ObjectId fileId = gridFsTemplate.store(
                        request.getFinalSignedFile().getInputStream(),
                        request.getFinalSignedFile().getOriginalFilename(),
                        request.getFinalSignedFile().getContentType()
                );

                if (latestReply == null) {
                    latestReply = DocumentReply.builder()
                            .documentId(documentId)
                            .replyText("Auto-generated reply by HEAD")
                            .preparedBy(head.getId())
                            .replyStatus(ReplyStatus.APPROVED)
                            .approvedBy(head.getId())
                            .approvedAt(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .replyType(ReplyType.STANDARD)
                            .build();
                    latestReply = documentReplyRepository.save(latestReply);
                }

                latestReply.setFinalSignedMongoFileId(fileId.toString());
                latestReply.setFinalSignedFileName(
                        request.getFinalSignedFile().getOriginalFilename());
                latestReply.setFinalSignedFileType(
                        request.getFinalSignedFile().getContentType());
                documentReplyRepository.save(latestReply);

            } catch (IOException e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to upload signed file");
            }
        }

        document.setCurrentDepartmentId(target.getDepartmentId());
        document.setAssignedTo(null);
        documentRepository.save(document);



        ragProcessingService.uploadToRagForNewUser(
                documentId,
                target.getId().toString()
        );

        notificationService.notifyHead(target.getDepartmentId(), documentId,
                "DOCUMENT_FORWARDED",
                "A new document has been forwarded to your department", "HIGH");

        autoLogService.log(head.getId(), documentId, "FORWARDED_TO_DEPARTMENT",
                "Forwarded to department: " + target.getDepartmentId(),
                target.getDepartmentId(), document.getStatus().name());
    }


    @Transactional
    public String createReply(UUID documentId, ReplyRequest request) {

        User head = getCurrentUser();
        validateHeadRole(head);

        if ((request.getReplyText() == null || request.getReplyText().isBlank())
                && (request.getReplyFile() == null
                || request.getReplyFile().isEmpty())) {
            throw DocumentException.noReplyContent();
        }

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        if (document.getCurrentDepartmentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document is not assigned to any department yet");
        }

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw DocumentException.accessDenied();
        }

        validateDocumentReady(document);

        if (document.getStatus() == DocumentStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Document already completed");
        }


        boolean hasHeadCreated = documentReplyRepository
                .existsByDocumentIdAndReplyStatus(
                        documentId, ReplyStatus.HEAD_CREATED);
        if (hasHeadCreated) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You already have a draft reply. Complete the document to send it.");
        }

        String mongoFileId = null;
        String fileName = null;
        String fileType = null;

        if (request.getReplyFile() != null &&
                !request.getReplyFile().isEmpty()) {
            try {
                ObjectId fileId = gridFsTemplate.store(
                        request.getReplyFile().getInputStream(),
                        request.getReplyFile().getOriginalFilename(),
                        request.getReplyFile().getContentType()
                );
                mongoFileId = fileId.toString();
                fileName = request.getReplyFile().getOriginalFilename();
                fileType = request.getReplyFile().getContentType();
            } catch (IOException e) {
                throw new RuntimeException("File upload failed");
            }
        }


        DocumentReply reply = DocumentReply.builder()
                .documentId(documentId)
                .replyText(request.getReplyText())
                .mongoFileId(mongoFileId)
                .fileName(fileName)
                .fileType(fileType)
                .preparedBy(head.getId())
                .replyStatus(ReplyStatus.HEAD_CREATED)
                .build();

        documentReplyRepository.save(reply);

        autoLogService.log(head.getId(), documentId, "REPLY_CREATED",
                "Head created reply - pending complete",
                head.getDepartmentId(), document.getStatus().name());

        return "Reply saved. Use 'Complete Document' to send it to the client.";
    }


    public List<ReplyResponseDTO> getPendingReplies(UUID documentId) {

        User head = getCurrentUser();
        validateHeadRole(head);

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        if (document.getCurrentDepartmentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document is not assigned to any department yet");
        }

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw DocumentException.accessDenied();
        }


        List<DocumentReply> replies = documentReplyRepository
                .findAllByDocumentIdAndReplyStatusOrderByCreatedAtDesc(
                        documentId, ReplyStatus.PENDING_APPROVAL);

        return replies.stream()
                .map(reply -> {
                    User creator = userRepository
                            .findById(reply.getPreparedBy())
                            .orElse(null);

                    return ReplyResponseDTO.builder()
                            .replyId(reply.getReplyId())
                            .documentId(documentId)
                            .replyText(reply.getReplyText())
                            .fileName(reply.getFileName())
                            .fileType(reply.getFileType())
                            .hasFile(reply.getMongoFileId() != null)
                            .replyStatus(reply.getReplyStatus().name())
                            .createdByName(creator != null
                                    ? creator.getUsername() : "Unknown")
                            .createdByEmail(creator != null
                                    ? creator.getEmail() : null)
                            .createdAt(reply.getCreatedAt())
                            .repliedAt(reply.getApprovedAt())
                            .build();
                })
                .toList();
    }


    @Transactional
    public String approveReply(UUID replyId, ReplyRequest request) {

        User head = getCurrentUser();
        validateHeadRole(head);

        DocumentReply reply = documentReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Reply not found"));

        if (reply.getReplyStatus() != ReplyStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reply already processed"
            );
        }

        if (reply.getReplyStatus() != ReplyStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This reply has already been processed");
        }

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(reply.getDocumentId())
                .orElseThrow(() -> DocumentException.notFound(
                        reply.getDocumentId()));

        if (document.getCurrentDepartmentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document is not assigned to any department yet");
        }

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw DocumentException.accessDenied();
        }


        if (request != null && request.getReplyFile() != null &&
                !request.getReplyFile().isEmpty()) {
            try {
                ObjectId fileId = gridFsTemplate.store(
                        request.getReplyFile().getInputStream(),
                        request.getReplyFile().getOriginalFilename(),
                        request.getReplyFile().getContentType()
                );
                reply.setFinalSignedMongoFileId(fileId.toString());
                reply.setFinalSignedFileName(
                        request.getReplyFile().getOriginalFilename());
                reply.setFinalSignedFileType(
                        request.getReplyFile().getContentType());
            } catch (IOException e) {
                throw new RuntimeException("File upload failed");
            }
        }


        reply.setReplyStatus(ReplyStatus.APPROVED);
        reply.setApprovedBy(head.getId());
        reply.setApprovedAt(LocalDateTime.now());


        documentReplyRepository.save(reply);


        documentRepository.save(document);


        notificationService.notify(reply.getPreparedBy(),
                document.getDocumentId(), "REPLY_APPROVED",
                "Your reply has been approved by the head", "HIGH");

        autoLogService.log(head.getId(), document.getDocumentId(),
                "REPLY_APPROVED", "Reply approved - awaiting complete",
                head.getDepartmentId(), document.getStatus().name());

        return "Reply approved. Use 'Complete Document' to send it to the client.";
    }
    @Transactional
    public String returnToEmployee(UUID replyId) {

        User head = getCurrentUser();
        validateHeadRole(head);

        DocumentReply reply = documentReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reply not found"
                ));

        if (reply.getReplyStatus() != ReplyStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only approved replies can be returned to employee"
            );
        }

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(reply.getDocumentId())
                .orElseThrow(() -> DocumentException.notFound(
                        reply.getDocumentId()
                ));

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw DocumentException.accessDenied();
        }

        document.setAssignedTo(reply.getPreparedBy());

        documentRepository.save(document);

        notificationService.notify(
                reply.getPreparedBy(),
                document.getDocumentId(),
                "DOCUMENT_RETURNED",
                "The document was returned to you for further processing",
                "HIGH"
        );

        autoLogService.log(
                head.getId(),
                document.getDocumentId(),
                "RETURNED_TO_EMPLOYEE",
                "Document returned to employee",
                head.getDepartmentId(),
                document.getStatus().name()
        );

        return "Document returned to employee successfully";
    }

    @Transactional
    public String rejectReply(UUID replyId, String rejectionReason) {

        User head = getCurrentUser();
        validateHeadRole(head);

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Reason required");
        }

        DocumentReply reply = documentReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Reply not found"));


        if (reply.getReplyStatus() != ReplyStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This reply has already been processed");
        }

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(reply.getDocumentId())
                .orElseThrow(() -> DocumentException.notFound(
                        reply.getDocumentId()));

        if (document.getCurrentDepartmentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document is not assigned to any department yet");
        }

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw DocumentException.accessDenied();
        }
        if (document.getStatus() == DocumentStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Document already completed");
        }

        reply.setReplyStatus(ReplyStatus.REJECTED);
        reply.setRejectionReason(rejectionReason);

        reply.setRejectedBy(head.getId());
        reply.setRejectedAt(LocalDateTime.now());
        documentReplyRepository.save(reply);


        document.setAssignedTo(reply.getPreparedBy());
        documentRepository.save(document);

        notificationService.notify(reply.getPreparedBy(),
                document.getDocumentId(), "REPLY_REJECTED",
                "Your reply has been rejected: " + rejectionReason, "HIGH");

        autoLogService.log(head.getId(), document.getDocumentId(),
                "REPLY_REJECTED", "Rejected: " + rejectionReason,
                head.getDepartmentId(), document.getStatus().name());

        return "Rejected";
    }


    @Transactional
    public String completeDocument(
            UUID documentId, CompleteDocumentRequest request) {

        User head = getCurrentUser();
        validateHeadRole(head);

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        if (document.getCurrentDepartmentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document is not assigned to any department yet");
        }

        if (!document.getCurrentDepartmentId().equals(head.getDepartmentId())) {
            throw DocumentException.accessDenied();
        }

        if (document.getStatus() == DocumentStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Document already completed");
        }

        DocumentReply absoluteLatest = documentReplyRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElse(null);

        if (absoluteLatest != null
                && absoluteLatest.getReplyStatus() == ReplyStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot complete: there is a pending reply awaiting approval");
        }

        DocumentReply reply = documentReplyRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .filter(r -> r.getReplyStatus() == ReplyStatus.APPROVED
                        || r.getReplyStatus() == ReplyStatus.HEAD_CREATED)
                .orElse(null);

        if (reply != null) {

            if (reply.getReplyText() == null || reply.getReplyText().isBlank()) {
                reply.setReplyText(request.getNotes());
            }

            if (request.getFinalFiles() != null &&
                    !request.getFinalFiles().isEmpty()) {

                MultipartFile firstFile = request.getFinalFiles().get(0);
                try {
                    ObjectId firstFileId = gridFsTemplate.store(
                            firstFile.getInputStream(),
                            firstFile.getOriginalFilename(),
                            firstFile.getContentType()
                    );
                    reply.setMongoFileId(firstFileId.toString());
                    reply.setFileName(firstFile.getOriginalFilename());
                    reply.setFileType(firstFile.getContentType());
                } catch (IOException e) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to upload final file");
                }

                for (int i = 1; i < request.getFinalFiles().size(); i++) {
                    MultipartFile extraFile = request.getFinalFiles().get(i);
                    try {
                        ObjectId extraFileId = gridFsTemplate.store(
                                extraFile.getInputStream(),
                                extraFile.getOriginalFilename(),
                                extraFile.getContentType()
                        );
                        DocumentReply extraReply = DocumentReply.builder()
                                .documentId(documentId)
                                .preparedBy(head.getId())
                                .replyStatus(ReplyStatus.APPROVED)
                                .approvedBy(head.getId())
                                .approvedAt(LocalDateTime.now())
                                .createdAt(LocalDateTime.now())
                                .replyType(ReplyType.STANDARD)

                                .mongoFileId(extraFileId.toString())
                                .fileName(extraFile.getOriginalFilename())
                                .fileType(extraFile.getContentType())
                                .build();
                        documentReplyRepository.save(extraReply);
                    } catch (IOException e) {
                        throw new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Failed to upload additional file");
                    }
                }
            }

            if (reply.getReplyStatus() == ReplyStatus.HEAD_CREATED) {
                reply.setReplyStatus(ReplyStatus.APPROVED);
                reply.setApprovedBy(head.getId());
                reply.setApprovedAt(LocalDateTime.now());
            }

            documentReplyRepository.save(reply);

        } else {
            DocumentReply newReply = DocumentReply.builder()
                    .documentId(documentId)
                    .replyText(request.getNotes())
                    .preparedBy(head.getId())
                    .replyStatus(ReplyStatus.APPROVED)
                    .approvedBy(head.getId())
                    .approvedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .replyType(ReplyType.STANDARD)
                    .build();

            if (request.getFinalFiles() != null &&
                    !request.getFinalFiles().isEmpty()) {
                MultipartFile firstFile = request.getFinalFiles().get(0);
                try {
                    ObjectId firstFileId = gridFsTemplate.store(
                            firstFile.getInputStream(),
                            firstFile.getOriginalFilename(),
                            firstFile.getContentType()
                    );

                    newReply.setMongoFileId(firstFileId.toString());
                    newReply.setFileName(firstFile.getOriginalFilename());
                    newReply.setFileType(firstFile.getContentType());
                } catch (IOException e) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to upload final file");
                }
            }

            documentReplyRepository.save(newReply);
        }

        document.setStatus(DocumentStatus.COMPLETED);
        document.setCompletedDate(LocalDateTime.now());
        document.setAssignedTo(null);
        document.setCurrentDepartmentId(null);
        documentRepository.save(document);

        if (document.getSubmittedBy() != null) {
            notificationService.notify(
                    document.getSubmittedBy(),
                    documentId,
                    "DOCUMENT_COMPLETED",
                    "Your document has been finalized and is ready for review",
                    "HIGH");
        }

        autoLogService.log(head.getId(), documentId, "DOCUMENT_COMPLETED",
                "Document completed and delivered. Notes: " + request.getNotes(),
                head.getDepartmentId(), DocumentStatus.COMPLETED.name());

        return "Document completed and delivered successfully";
    }
    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    private void validateHeadRole(User user) {
        if (!user.getRole().equals("HEAD")) {
            throw DocumentException.accessDenied();
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private DocumentResponseDTO toResponseDTO(Document document) {

        DocumentReply latestReply = documentReplyRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(
                        document.getDocumentId()
                )
                .orElse(null);

        return DocumentResponseDTO.builder()
                .documentId(document.getDocumentId())
                .documentFormat(document.getDocumentFormat())

                .status(
                        document.getStatus() != null
                                ? document.getStatus().name()
                                : "—"
                )

                .submittedDate(document.getSubmittedDate())
                .lastUpdated(document.getLastUpdated())
                .confidential(document.getConfidential())

                .latestReplyStatus(
                        latestReply != null
                                ? latestReply.getReplyStatus().name()
                                : null
                )

                .latestReplyId(
                        latestReply != null
                                ? latestReply.getReplyId()
                                : null
                )


                .rejectionReason(
                        latestReply != null
                                ? latestReply.getRejectionReason()
                                : null
                )

                .build();
    }

    private void validateDocumentReady(Document document) {
        if (document.getConfidential() != null &&
                document.getConfidential()) return;
        if (document.getAiProcessingStatus() == null ||
                document.getAiProcessingStatus() != AiProcessingStatus.AI_DONE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Document not ready");
        }
    }

}