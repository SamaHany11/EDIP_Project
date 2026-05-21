package com.example.EDIP.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSearchCriteria {

    // Keyword search
    private String keyword;

    // Document statuses: PENDING, COMPLETED
    private List<String> statuses;

    // Date range
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Department filtering
    private List<UUID> departmentIds;

    // Submitted by specific users
    private UUID submittedBy;

    // Assigned to specific users
    private List<UUID> assignedToUserIds;

    // Flag for role-based filtering
    private boolean includeDeleted = false;

    /**
     * Normalize statuses - convert to uppercase if provided
     */
    public List<String> getNormalizedStatuses() {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return statuses.stream()
                .map(String::toUpperCase)
                .toList();
    }

    /**
     * Check if date range is valid
     */
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true; // No date filter
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Get safe keyword for LIKE queries
     */
    public String getSafeKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return "%";
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}