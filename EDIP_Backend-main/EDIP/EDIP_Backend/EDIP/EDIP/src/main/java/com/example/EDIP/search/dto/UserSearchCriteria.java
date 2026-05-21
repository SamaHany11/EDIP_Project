package com.example.EDIP.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchCriteria {

    // Keyword search (username, email)
    private String keyword;

    // Role filtering: HEAD, EMPLOYEE, ADMIN
    private List<String> roleFilters;

    // Department IDs to search in
    private List<UUID> departmentIds;

    // Specific user IDs (for targeted search)
    private List<UUID> userIds;

    // Exclude specific users (e.g., exclude current user)
    private List<UUID> excludeUserIds;

    // Filter by active status only
    private Boolean activeOnly = true;

    /**
     * Normalize roles - convert to uppercase if provided
     */
    public List<String> getNormalizedRoles() {
        if (roleFilters == null || roleFilters.isEmpty()) {
            return null;
        }
        return roleFilters.stream()
                .map(String::toUpperCase)
                .toList();
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

    /**
     * Check if any filters are applied
     */
    public boolean hasFilters() {
        return (keyword != null && !keyword.isBlank())
                || (roleFilters != null && !roleFilters.isEmpty())
                || (departmentIds != null && !departmentIds.isEmpty())
                || (userIds != null && !userIds.isEmpty())
                || (excludeUserIds != null && !excludeUserIds.isEmpty());
    }
}