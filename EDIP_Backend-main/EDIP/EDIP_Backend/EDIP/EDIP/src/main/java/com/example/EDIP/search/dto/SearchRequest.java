package com.example.EDIP.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchRequest {

    // Basic search
    private String keyword = "";

    // Pagination
    private int page = 0;
    private int size = 10;

    // Sorting
    private String sortBy = "submittedDate";
    private String sortDirection = "DESC";

    // Type: ALL, DOCS, USERS
    private String type = "ALL";

    // ========== Document Filters ==========

    // Document status: PENDING, COMPLETED
    private List<String> statuses;

    // Date range filtering
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Department filter by UUID (for Admin)
    private List<UUID> departmentIds;

    // Department filter by name — partial match, case-insensitive (for Admin)
    private String departmentName;

    // Assigned to specific users
    private List<UUID> assignedToUserIds;

    // ========== User Filters ==========

    // Role filter: HEAD, EMPLOYEE (for Admin and Head)
    private List<String> roleFilters;

    // Specific user IDs to search (for Admin)
    private List<UUID> userIds;

    // Exclude specific users
    private List<UUID> excludeUserIds;

    private String docScope;
}