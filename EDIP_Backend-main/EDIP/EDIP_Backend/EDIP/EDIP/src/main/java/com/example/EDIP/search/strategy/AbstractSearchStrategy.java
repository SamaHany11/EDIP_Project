package com.example.EDIP.search.strategy;

import com.example.EDIP.search.dto.DocumentSearchCriteria;
import com.example.EDIP.search.dto.UserSearchCriteria;
import com.example.EDIP.search.dto.ResultWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

public abstract class AbstractSearchStrategy {

    // ========== Pageable Builders ==========

    /**
     * Build Pageable for Document sorting
     */
    protected Pageable buildDocumentPageable(int page, int size, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), "submittedDate");
        return PageRequest.of(page, size, sort);
    }

    /**
     * Build Pageable for User sorting
     */
    protected Pageable buildUserPageable(int page, int size, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), "createdAt");
        return PageRequest.of(page, size, sort);
    }

    // ========== Criteria Builders ==========

    /**
     * Build DocumentSearchCriteria from request parameters
     */
    protected DocumentSearchCriteria buildDocumentCriteria(
            String keyword,
            List<String> statuses,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<java.util.UUID> departmentIds
    ) {
        DocumentSearchCriteria criteria = DocumentSearchCriteria.builder()
                .keyword(keyword != null ? keyword : "")
                .statuses(statuses)
                .departmentIds(departmentIds)
                .build();

        // Validate and set date range
        if (startDate != null && endDate != null) {
            if (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {
                criteria.setStartDate(startDate);
                criteria.setEndDate(endDate);
            }
        }

        return criteria;
    }

    /**
     * Build UserSearchCriteria from request parameters
     */
    protected UserSearchCriteria buildUserCriteria(
            String keyword,
            List<String> roleFilters,
            List<java.util.UUID> departmentIds,
            List<java.util.UUID> excludeUserIds
    ) {
        return UserSearchCriteria.builder()
                .keyword(keyword != null ? keyword : "")
                .roleFilters(roleFilters)
                .departmentIds(departmentIds)
                .excludeUserIds(excludeUserIds)
                .activeOnly(true)
                .build();
    }

    // ========== Result Wrappers ==========

    /**
     * Wrap Page result into ResultWrapper
     */
    protected <T, R> ResultWrapper<R> wrap(Page<T> pg, java.util.function.Function<T, R> mapper) {
        return new ResultWrapper<>(
                pg.getContent().stream().map(mapper).toList(),
                pg.getTotalElements(),
                pg.getNumber(),
                pg.getSize(),
                pg.getTotalPages()
        );
    }

    /**
     * Create empty ResultWrapper
     */
    protected <R> ResultWrapper<R> emptyWrapper(int page, int size) {
        return new ResultWrapper<>(
                List.of(),
                0,
                page,
                size,
                0
        );
    }

    // ========== Validation Helpers ==========

    /**
     * Validate date range
     */
    protected boolean isValidDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return true; // No date filter
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Get safe keyword for LIKE queries
     */
    protected String getSafeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "%";
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}