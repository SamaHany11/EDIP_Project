package com.example.EDIP.document.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.document.dto.response.NotificationDTO;
import com.example.EDIP.document.dto.response.PagedResponse;
import com.example.EDIP.document.model.sql.Notification;
import com.example.EDIP.document.repository.sql.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;


    @Async
    public void notify(
            UUID userId,
            UUID documentId,
            String notificationType,
            String content,
            String priority
    ) {
        try {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .documentId(documentId)
                    .notificationType(notificationType)
                    .content(content)
                    .priority(priority)
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);

            log.info("Notification sent to user: {} - {}",
                    userId, notificationType);

        } catch (Exception e) {
            log.error("Notification failed: {} - {}",
                    notificationType, e.getMessage());
        }
    }

    @Async
    public void notifyHead(
            UUID departmentId,
            UUID documentId,
            String notificationType,
            String content,
            String priority
    ) {
        try {

            userRepository.findHeadByDepartmentId(departmentId)
                    .ifPresent(head -> {
                        Notification notification = Notification.builder()
                                .userId(head.getId())
                                .documentId(documentId)
                                .notificationType(notificationType)
                                .content(content)
                                .priority(priority)
                                .isRead(false)
                                .build();

                        notificationRepository.save(notification);

                        log.info("Notification sent to head: {} - {}",
                                head.getId(), notificationType);
                    });

        } catch (Exception e) {
            log.error("Head notification failed: {} - {}",
                    notificationType, e.getMessage());
        }
    }


    private NotificationDTO mapToDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setNotificationId(n.getNotificationId());
        dto.setDocumentId(n.getDocumentId());
        dto.setNotificationType(n.getNotificationType());
        dto.setContent(n.getContent());
        dto.setPriority(n.getPriority());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt().toString());
        return dto;
    }

    private PagedResponse<NotificationDTO> buildResponse(Page<Notification> page) {

        List<NotificationDTO> content = page.getContent()
                .stream()
                .map(this::mapToDTO)
                .toList();

        PagedResponse<NotificationDTO> response = new PagedResponse<>();
        response.setContent(content);
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());

        return response;
    }


    public PagedResponse<NotificationDTO> getMyNotifications(Pageable pageable) {

        User currentUser = getCurrentUser();

        Page<Notification> page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

        return buildResponse(page);
    }


    public PagedResponse<NotificationDTO> getUnreadNotifications(Pageable pageable) {

        User currentUser = getCurrentUser();

        Page<Notification> page = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
                        currentUser.getId(), pageable);

        return buildResponse(page);
    }


    public long getUnreadCount() {
        User currentUser = getCurrentUser();
        return notificationRepository
                .countByUserIdAndIsReadFalse(currentUser.getId());
    }

    // ─────────────────────────────────────────────
    // Mark as Read
    // ─────────────────────────────────────────────
    public void markAsRead(UUID notificationId) {

        User currentUser = getCurrentUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Notification not found"
                ));

        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        notification.setRead(true);
        notificationRepository.save(notification);
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
}