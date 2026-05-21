package com.example.EDIP.document.service;

import com.example.EDIP.document.dto.response.AuditLogSummaryDTO;
import com.example.EDIP.document.model.sql.ActionLabels;
import com.example.EDIP.document.model.sql.AutoLog;
import com.example.EDIP.document.repository.sql.AutoLogRepository;
import com.example.EDIP.document.dto.response.AuditLogResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.EDIP.Auth.model.Department;
import com.example.EDIP.Auth.repository.DepartmentRepository;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.document.repository.sql.DocumentRepository;

import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoLogService {

    private final AutoLogRepository autoLogRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DocumentRepository documentRepository;


    @Async
    public void log(
            UUID userId,
            UUID documentId,
            String actionType,
            String details,
            UUID departmentId,
            String documentStatus,
            String ipAddress
    ) {
        try {
            Document document = documentRepository
                    .findById(documentId)
                    .orElse(null);

            if (document != null && Boolean.TRUE.equals(document.getConfidential())) {
                return;
            }

            AutoLog autoLog = AutoLog.builder()
                    .userId(userId)
                    .documentId(documentId)
                    .actionType(actionType)
                    .actionLabel(ActionLabels.label(actionType))
                    .details(details)
                    .departmentId(departmentId)
                    .documentStatus(documentStatus)
                    .build();

            autoLogRepository.save(autoLog);

        } catch (Exception e) {
            log.error("AutoLog failed: {}", e.getMessage());
        }
    }


    @Async
    public void log(
            UUID userId,
            UUID documentId,
            String actionType,
            String details,
            UUID departmentId,
            String documentStatus
    ) {
        log(userId, documentId, actionType, details,
                departmentId, documentStatus, null);
    }


    public Page<AuditLogResponseDTO> getAuditLogs(UUID departmentId, Pageable pageable) {

        User user = getCurrentUser();
        String role = user.getRole();

        if ("EMPLOYEE".equals(role)) {
            return autoLogRepository
                    .findByUserIdOrderByActionTimestampDesc(user.getId(), pageable)
                    .map(this::toResponseDTO);
        }

        if ("HEAD".equals(role)) {
            return autoLogRepository
                    .findByDepartmentIdOrderByActionTimestampDesc(
                            user.getDepartmentId(), pageable)
                    .map(this::toResponseDTO);
        }

        if ("ADMIN".equals(role)) {
            if (departmentId != null) {
                return autoLogRepository
                        .findByDepartmentIdOrderByActionTimestampDesc(departmentId, pageable)
                        .map(this::toResponseDTO);
            }
            return autoLogRepository
                    .findAllByOrderByActionTimestampDesc(pageable)
                    .map(this::toResponseDTO);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }


    public Page<AuditLogResponseDTO> getAuditLogsByName(String departmentName, Pageable pageable) {

        User user = getCurrentUser();
        String role = user.getRole();

        UUID departmentId = null;

        if (departmentName != null && !departmentName.isBlank()) {
            Department dept = departmentRepository
                    .findByDepartmentNameIgnoreCase(departmentName)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Department not found: " + departmentName
                    ));
            departmentId = dept.getDepartmentId();
        }

        if ("EMPLOYEE".equals(role)) {
            return autoLogRepository
                    .findByUserIdOrderByActionTimestampDesc(user.getId(), pageable)
                    .map(this::toResponseDTO);
        }

        if ("HEAD".equals(role)) {
            return autoLogRepository
                    .findByDepartmentIdOrderByActionTimestampDesc(
                            user.getDepartmentId(), pageable)
                    .map(this::toResponseDTO);
        }

        if ("ADMIN".equals(role)) {
            if (departmentId != null) {
                return autoLogRepository
                        .findByDepartmentIdOrderByActionTimestampDesc(departmentId, pageable)
                        .map(this::toResponseDTO);
            }
            return autoLogRepository
                    .findAllByOrderByActionTimestampDesc(pageable)
                    .map(this::toResponseDTO);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }


    private AuditLogResponseDTO toResponseDTO(AutoLog log) {

        String performedBy = (log.getUserId() != null)
                ? userRepository.findById(log.getUserId())
                .map(User::getUsername)
                .orElse("Unknown")
                : "System";

        String departmentName = (log.getDepartmentId() != null)
                ? departmentRepository.findById(log.getDepartmentId())
                .map(Department::getDepartmentName)
                .orElse("Unknown")
                : "—";

        String formattedStatus = formatStatus(log.getDocumentStatus());

        String timestamp = (log.getActionTimestamp() != null)
                ? log.getActionTimestamp()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "—";

        return AuditLogResponseDTO.builder()
                .documentId(log.getDocumentId())
                .actionLabel(log.getActionLabel() != null
                        ? log.getActionLabel()
                        : ActionLabels.label(log.getActionType()))
                .performedBy(performedBy)
                .departmentName(departmentName)
                .documentStatus(formattedStatus)
                .details(log.getDetails())
                .timestamp(log.getActionTimestamp())
                .build();
    }


    private String formatStatus(String status) {
        if (status == null) return "—";
        return switch (status) {
            case "SUBMITTED" -> "Submitted";
            case "PENDING"   -> "Pending";
            case "COMPLETED" -> "Completed";
            default          -> status;
        };
    }

    public List<AuditLogResponseDTO> getDocumentAuditLogs(UUID documentId) {

        User user = getCurrentUser();

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        // Security check
        String role = user.getRole();
        if ("EXTERNAL".equals(role)) {
            if (!document.getSubmittedBy().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        return autoLogRepository
                .findByDocumentIdOrderByActionTimestampAsc(documentId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
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

    public Page<AuditLogSummaryDTO> getAuditLogsSummary(String departmentName, Pageable pageable) {

        User user = getCurrentUser();
        String role = user.getRole();

        UUID departmentId = null;

        if ("ADMIN".equals(role)) {
            if (departmentName != null && !departmentName.isBlank()) {
                Department dept = departmentRepository.findAll()
                        .stream()
                        .filter(d -> d.getDepartmentName() != null &&
                                d.getDepartmentName()
                                        .replaceAll("\\s+", "")
                                        .equalsIgnoreCase(
                                                departmentName.replaceAll("\\s+", "")
                                        ))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Department not found: " + departmentName
                        ));

                departmentId = dept.getDepartmentId();
            }
        }

        List<UUID> documentIds;

        if ("EMPLOYEE".equals(role)) {

            documentIds = autoLogRepository
                    .findDistinctDocumentIdsByUserId(user.getId());

        } else if ("HEAD".equals(role)) {

            documentIds = autoLogRepository
                    .findDistinctDocumentIdsByDepartmentId(user.getDepartmentId());

        } else if ("ADMIN".equals(role)) {

            if (departmentId != null) {
                documentIds = autoLogRepository
                        .findDistinctDocumentIdsByDepartmentId(departmentId);
            } else {
                documentIds = autoLogRepository.findDistinctDocumentIds();
            }

        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<AuditLogSummaryDTO> list = documentIds.stream()
                .map(docId -> {

                    long total = autoLogRepository.countByDocumentId(docId);

                    AutoLog last = autoLogRepository
                            .findTopByDocumentIdOrderByActionTimestampDesc(docId);

                    String fileName = documentRepository
                            .findById(docId)
                            .map(Document::getFileName)
                            .orElse("Unknown");

                    return AuditLogSummaryDTO.builder()
                            .documentId(docId)
                            .fileName(fileName)
                            .totalActions((int) total)
                            .lastAction(last != null
                                    ? ActionLabels.label(last.getActionType())
                                    : "—")
                            .lastActionBy(last != null
                                    ? userRepository.findById(last.getUserId())
                                    .map(User::getUsername)
                                    .orElse("Unknown")
                                    : "—")
                            .lastActionTime(last != null
                                    ? last.getActionTimestamp()
                                    : null)
                            .build();
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        List<AuditLogSummaryDTO> page = (start >= list.size())
                ? List.of()
                : list.subList(start, end);

        return new PageImpl<>(page, pageable, list.size());
    }
}