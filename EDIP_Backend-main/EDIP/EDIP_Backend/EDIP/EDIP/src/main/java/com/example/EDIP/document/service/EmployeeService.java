package com.example.EDIP.document.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.document.dto.request.ForwardToColleagueRequest;
import com.example.EDIP.document.dto.request.ReplyRequest;
import com.example.EDIP.document.dto.response.DocumentResponseDTO;
import com.example.EDIP.document.exception.DocumentException;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.document.model.sql.DocumentReply;
import com.example.EDIP.document.model.sql.DocumentStatus;
import com.example.EDIP.document.model.sql.ReplyStatus;
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
import org.springframework.web.server.ResponseStatusException;
import com.example.EDIP.chatbot.service.RagProcessingService;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final DocumentRepository documentRepository;
    private final DocumentReplyRepository documentReplyRepository;
    private final UserRepository userRepository;
    private final AutoLogService autoLogService;
    private final NotificationService notificationService;
    private final GridFsTemplate gridFsTemplate;
    private final RagProcessingService ragProcessingService;

    @Transactional
    public DocumentResponseDTO forwardToColleague(
            UUID documentId,
            ForwardToColleagueRequest request
    ) {
        User employee = getCurrentUser();
        validateEmployeeRole(employee);

        // 1. Get document
        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        if (Boolean.TRUE.equals(document.getConfidential())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access or forward confidential documents"
            );
        }

        // 2. Check ownership
        if (!document.getAssignedTo().equals(employee.getId())) {
            throw DocumentException.accessDenied();
        }

        DocumentReply lastReply = documentReplyRepository
                .findTopByDocumentIdAndPreparedByOrderByCreatedAtDesc(
                        documentId,
                        employee.getId()
                )
                .orElse(null);


        if (lastReply != null &&
                lastReply.getReplyStatus() == ReplyStatus.PENDING_APPROVAL) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot forward this document until head reviews your reply"
            );
        }


        if (lastReply != null &&
                lastReply.getReplyStatus() == ReplyStatus.REJECTED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Your latest reply was rejected. Submit a new approved reply first"
            );
        }

        // 3. Get colleague
        User colleague = userRepository.findByEmail(request.getColleagueEmail())
                .orElseThrow(() -> new RuntimeException("Colleague not found"));


        // 4. Check same department
        if (!colleague.getDepartmentId().equals(employee.getDepartmentId())) {
            throw DocumentException.cannotForwardOutsideDepartment();
        }

        // 5. Update document
        document.setAssignedTo(colleague.getId());
        documentRepository.save(document);


        ragProcessingService.uploadToRagForNewUser(
                documentId,
                colleague.getId().toString()
        );


        autoLogService.log(
                employee.getId(),
                documentId,
                "FORWARDED_TO_COLLEAGUE",
                "Forwarded to: " + colleague.getEmail() +
                        (request.getNotes() != null ? " | Notes: " + request.getNotes() : ""),
                employee.getDepartmentId(),
                document.getStatus().name()
        );

        // 7. Notification
        notificationService.notify(
                colleague.getId(),

                documentId,
                "DOCUMENT_ASSIGNED",
                "A document has been assigned to you by a colleague for your review",
                "MEDIUM"
        );

        log.info("Document {} forwarded from {} to {}",
                documentId,
                employee.getEmail(),
                colleague.getEmail()
        );

        return toResponseDTO(document);
    }

    @Transactional
    public String createReply(
            UUID documentId,
            ReplyRequest request
    ) {
        User employee = getCurrentUser();
        validateEmployeeRole(employee);

        if ((request.getReplyText() == null || request.getReplyText().isBlank())
                && (request.getReplyFile() == null || request.getReplyFile().isEmpty())) {
            throw DocumentException.noReplyContent();
        }

        Document document = documentRepository
                .findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> DocumentException.notFound(documentId));

        if (Boolean.TRUE.equals(document.getConfidential())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot reply to confidential documents"
            );
        }

        if (document.getStatus() == DocumentStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot submit a reply. Document is already completed.");
        }

        if (document.getAssignedTo() == null ||
                !document.getAssignedTo().equals(employee.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This document is not assigned to you.");
        }


        boolean hasPendingReply = documentReplyRepository
                .existsByDocumentIdAndPreparedByAndReplyStatus(
                        documentId,
                        employee.getId(),
                        ReplyStatus.PENDING_APPROVAL
                );

        if (hasPendingReply) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A reply is already pending approval"
            );
        }

        String mongoFileId = null;
        String fileName = null;
        String fileType = null;

        if (request.getReplyFile() != null && !request.getReplyFile().isEmpty()) {
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
                throw DocumentException.invalidFile("Failed to save reply file");
            }
        }

        DocumentReply reply = DocumentReply.builder()
                .documentId(documentId)
                .replyText(request.getReplyText())
                .mongoFileId(mongoFileId)
                .fileName(fileName)
                .fileType(fileType)
                .preparedBy(employee.getId())
                .replyStatus(ReplyStatus.PENDING_APPROVAL)
                .build();


        documentReplyRepository.save(reply);


        autoLogService.log(
                employee.getId(),
                documentId,
                "REPLY_CREATED",
                "Reply submitted successfully",
                employee.getDepartmentId(),
                document.getStatus().name()
        );

        notificationService.notifyHead(
                employee.getDepartmentId(),
                documentId,
                "REPLY_PENDING_APPROVAL",
                "A new reply has been submitted and is waiting for your review",
                "HIGH"
        );

        return "Reply submitted successfully and sent to head for review";
    }

    // ─────────────────────────────────────────────
    // Validate Employee Role
    // ─────────────────────────────────────────────
    private void validateEmployeeRole(User user) {
        if (!user.getRole().equals("EMPLOYEE")) {
            throw DocumentException.accessDenied();
        }
    }

    // ─────────────────────────────────────────────
    // Get Current User
    // ─────────────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─────────────────────────────────────────────
    // Map to Response DTO
    // ─────────────────────────────────────────────
    private DocumentResponseDTO toResponseDTO(Document document) {
        return DocumentResponseDTO.builder()
                .documentId(document.getDocumentId())
                .documentFormat(document.getDocumentFormat())
                .status(document.getStatus() != null
                        ? document.getStatus().name() : "—")
                .submittedDate(document.getSubmittedDate())
                .lastUpdated(document.getLastUpdated())
                .confidential(document.getConfidential())

                .build();
    }
}