package com.example.EDIP.document.controller;

import com.example.EDIP.document.dto.request.*;
import com.example.EDIP.document.dto.response.*;
import com.example.EDIP.document.model.sql.AutoLog;
import com.example.EDIP.document.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.EDIP.document.model.sql.Notification;
import com.example.EDIP.document.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final TrackingService trackingService;
    private final HeadService headService;
    private final EmployeeService employeeService;
    private final NotificationService notificationService;
    private final AutoLogService autoLogService;


    @PostMapping(
            value = "/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('EXTERNAL','EMPLOYEE','HEAD','ADMIN')")
    public ResponseEntity<Map<String, Object>> submitDocument(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("confidential") boolean confidential,
            @RequestParam(value = "targetUserEmail", required = false) String targetUserEmail
    ) {

        SubmitDocumentRequest request = new SubmitDocumentRequest();
        request.setFiles(files);

        request.setConfidential(confidential);
        request.setTargetUserEmail(targetUserEmail);

        DocumentResponseDTO response = documentService.submitDocument(request);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Document submitted successfully and processing started");
        result.put("documentId", response.getDocumentId());

        return ResponseEntity.ok(result);
    }



    @GetMapping("/my-documents")
    @PreAuthorize("hasAnyRole('EXTERNAL','EMPLOYEE','HEAD','ADMIN')")
    public ResponseEntity<Page<DocumentResponseDTO>> getMyDocuments(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Page<DocumentResponseDTO> response =
                documentService.getMyDocuments(type, pageable);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/department-documents")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HEAD')")
    public ResponseEntity<Page<DocumentResponseDTO>> getDepartmentDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("submittedDate").descending()
        );
        Page<DocumentResponseDTO> response =
                documentService.getDepartmentDocuments(pageable);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}/file")
    @PreAuthorize("hasAnyRole('EXTERNAL', 'EMPLOYEE', 'HEAD', 'ADMIN')")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable UUID id
    ) {
        return documentService.downloadFile(id);
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD','EMPLOYEE')")
    public ResponseEntity<Map<String, String>> getDocumentSummary(
            @PathVariable UUID id
    ) {
        String summary = documentService.getDocumentSummary(id);
        Map<String, String> response = new HashMap<>();
        response.put("summary", summary);
        return ResponseEntity.ok(response);
    }

    // download attachment
    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable UUID attachmentId) {
        return documentService.downloadAttachment(attachmentId);
    }

    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasRole('EXTERNAL')")
    public ResponseEntity<List<TrackingResponseDTO>> getTracking(
            @PathVariable UUID id
    ) {
        List<TrackingResponseDTO> tracking =
                trackingService.getDocumentTracking(id);
        return ResponseEntity.ok(tracking);
    }


    @PutMapping("/{id}/assign-to-employee")
    @PreAuthorize("hasRole('HEAD')")
    public ResponseEntity<Map<String, String>> assignToEmployee(
            @PathVariable UUID id,
            @RequestBody AssignToEmployeeRequest request
    ) {
        DocumentResponseDTO result = headService.assignToEmployee(id, request);

        Map<String, String> response = new HashMap<>();

        if (result == null) {
            response.put("message", "This document is already assigned to this employee");
        } else {
            response.put("message", "Document has been successfully assigned to the employee");
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping(
            value = "/{id}/forward-to-department",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('HEAD')")
    public ResponseEntity<Map<String, String>> forwardToDepartment(
            @PathVariable UUID id,
            @RequestParam String departmentEmail,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) MultipartFile finalSignedFile
    ) {

        ForwardToDepartmentRequest request = new ForwardToDepartmentRequest();
        request.setDepartmentEmail(departmentEmail);
        request.setNotes(notes);
        request.setFinalSignedFile(finalSignedFile);

        headService.forwardToDepartment(id, request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Document forwarded successfully");

        return ResponseEntity.ok(response);
    }


    @PutMapping("/replies/{replyId}/approve")
    @PreAuthorize("hasRole('HEAD')")
    public ResponseEntity<Map<String, String>> approveReply(
            @PathVariable UUID replyId,
            @ModelAttribute ReplyRequest request
    ) {
        String result = headService.approveReply(replyId, request);

        return ResponseEntity.ok(
                Map.of("message", result)
        );
    }

    @PutMapping("/replies/{replyId}/return-to-employee")
    @PreAuthorize("hasRole('HEAD')")
    public ResponseEntity<Map<String, String>> returnToEmployee(
            @PathVariable UUID replyId
    ) {

        String result = headService.returnToEmployee(replyId);

        return ResponseEntity.ok(
                Map.of("message", result)
        );
    }

    @PutMapping("/replies/{replyId}/reject")
    @PreAuthorize("hasRole('HEAD')")
    public ResponseEntity<Map<String, String>> rejectReply(
            @PathVariable UUID replyId,
            @RequestParam String reason
    ) {
        try {
            String result = headService.rejectReply(replyId, reason);

            Map<String, String> response = new HashMap<>();
            response.put("message", result);

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException ex) {

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getReason());

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(errorResponse);
        }
    }

    @PutMapping("/{id}/forward-to-colleague")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> forwardToColleague(
            @PathVariable UUID id,
            @RequestBody ForwardToColleagueRequest request
    ) {
        DocumentResponseDTO result =
                employeeService.forwardToColleague(id, request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Document forwarded successfully to colleague");


        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/{id}/create-reply",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('HEAD','EMPLOYEE')")
    public ResponseEntity<Map<String, String>> createReply(
            @PathVariable UUID id,
            @RequestParam(required = false) String replyText,
            @RequestParam(required = false) MultipartFile replyFile
    ) {
        ReplyRequest request = new ReplyRequest();
        request.setReplyText(replyText);
        request.setReplyFile(replyFile);


        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isHead = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HEAD"));

        String message;
        if (isHead) {
            message = headService.createReply(id, request);
        } else {
            message = employeeService.createReply(id, request);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    // GET /api/documents/notifications/all
    @GetMapping("/notifications/all")
    @PreAuthorize("hasAnyRole('EXTERNAL', 'EMPLOYEE', 'HEAD', 'ADMIN')")
    public ResponseEntity<PagedResponse<NotificationDTO>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                notificationService.getMyNotifications(pageable));
    }

    // GET /api/documents/notifications/unread
    @GetMapping("/notifications/unread")
    @PreAuthorize("hasAnyRole('EXTERNAL', 'EMPLOYEE', 'HEAD', 'ADMIN')")
    public ResponseEntity<PagedResponse<NotificationDTO>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(pageable));
    }

    // GET /api/documents/notifications/count
    @GetMapping("/notifications/unread-count")
    @PreAuthorize("hasAnyRole('EXTERNAL', 'EMPLOYEE', 'HEAD', 'ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long count = notificationService.getUnreadCount();

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    // PUT /api/documents/notifications/{id}/read
    @PutMapping("/notifications/{id}/mark-as-read")
    @PreAuthorize("hasAnyRole('EXTERNAL', 'EMPLOYEE', 'HEAD', 'ADMIN')")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable UUID id) {

        notificationService.markAsRead(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "success");

        return ResponseEntity.ok(response);
    }


    // ─────────────────────────────────────────────

// ─────────────────────────────────────────────




    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasAnyRole('EXTERNAL','EMPLOYEE','HEAD','ADMIN')")
    public ResponseEntity<Map<String, String>> resubmit(@PathVariable UUID id) {
        documentService.resubmitDocument(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Document resubmitted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HEAD','ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponseDTO>> getAuditLogs(
            @RequestParam(required = false) String departmentName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<AuditLogResponseDTO> result =
                autoLogService.getAuditLogsByName(departmentName, pageable);

        PageResponse<AuditLogResponseDTO> response = new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.hasNext(),
                result.hasPrevious()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HEAD','ADMIN')")
    public ResponseEntity<List<AuditLogResponseDTO>> getDocumentAuditLogs(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                autoLogService.getDocumentAuditLogs(id)
        );
    }

    @GetMapping("/audit-logs/summary")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HEAD','ADMIN')")
    public ResponseEntity<PageResponse<AuditLogSummaryDTO>> getAuditLogsSummary(
            @RequestParam(required = false) String departmentName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<AuditLogSummaryDTO> result =
                autoLogService.getAuditLogsSummary(departmentName, pageable);

        PageResponse<AuditLogSummaryDTO> response = new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.hasNext(),
                result.hasPrevious()
        );

        return ResponseEntity.ok(response);
    }



    private final WordPreviewService wordPreviewService;

    // ─────────────────────────────────────────────
// GET /api/documents/{id}/preview/word-html
// ─────────────────────────────────────────────
    @GetMapping("/word-preview")
    @PreAuthorize("hasAnyRole('EXTERNAL','EMPLOYEE','HEAD','ADMIN')")
    public ResponseEntity<Map<String, Object>> getWordPreview(
            @RequestParam(required = false) UUID documentId,
            @RequestParam(required = false) UUID attachmentId,
            @RequestParam(required = false) UUID replyId
    ) {

        int count = 0;

        if (documentId != null) count++;
        if (attachmentId != null) count++;
        if (replyId != null) count++;

        if (count == 0) {
            throw new RuntimeException(
                    "documentId or attachmentId or replyId must be provided"
            );
        }

        if (count > 1) {
            throw new RuntimeException(
                    "Provide only one parameter"
            );
        }

        log.info(
                "Word preview request: documentId={}, attachmentId={}, replyId={}",
                documentId,
                attachmentId,
                replyId
        );

        return ResponseEntity.ok(
                wordPreviewService.convertToHtml(
                        documentId,
                        attachmentId,
                        replyId
                )
        );
    }

    @GetMapping("/confidential")
    @PreAuthorize("hasAnyRole('HEAD','ADMIN')")
    public ResponseEntity<Page<DocumentResponseDTO>> getConfidentialDocs(Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("submittedDate").descending()
        );

        Page<DocumentResponseDTO> response =
                documentService.getConfidentialDocuments(sortedPageable);

        return ResponseEntity.ok(response);
    }



    @GetMapping("/{id}/replies/pending")
    @PreAuthorize("hasRole('HEAD')")
    public ResponseEntity<List<ReplyResponseDTO>> getPendingReplies(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(headService.getPendingReplies(id));
    }


    @GetMapping("/my-replies")
    @PreAuthorize("hasRole('EXTERNAL')")
    public ResponseEntity<List<ReplyResponseDTO>> getMyApprovedReplies() {
        return ResponseEntity.ok(documentService.getMyApprovedReplies());
    }


    @PutMapping(
            value = "/{id}/complete",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('HEAD')")
    public ResponseEntity<Map<String, String>> completeDocument(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            @RequestParam(required = true) List<MultipartFile> finalFiles
    ) {
        CompleteDocumentRequest request = new CompleteDocumentRequest();
        request.setNotes(notes);
        request.setFinalFiles(finalFiles);

        String result = headService.completeDocument(id, request);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @GetMapping("/replies/{replyId}/file")
    @PreAuthorize("hasAnyRole('HEAD','EMPLOYEE','EXTERNAL','ADMIN')")
    public ResponseEntity<byte[]> downloadReplyFileById(@PathVariable UUID replyId) {
        return documentService.downloadReplyFileById(replyId);
    }
    @GetMapping("/my-department")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HEAD')")
    public ResponseEntity<Map<String, String>> getMyDepartment() {
        return ResponseEntity.ok(documentService.getMyDepartment());
    }
    @GetMapping("/{id}/replies/all")
    @PreAuthorize("hasAnyRole('HEAD', 'ADMIN')")
    public ResponseEntity<List<ReplyResponseDTO>> getAllReplies(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getAllReplies(id));
    }
    @GetMapping("/{id}/reply/latest")
    @PreAuthorize("hasAnyRole('HEAD','ADMIN')")
    public ResponseEntity<ReplyResponseDTO> getLatestReply(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                documentService.getLatestReply(id)
        );
    }
}