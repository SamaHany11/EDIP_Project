package com.example.EDIP.search.controller;

import com.example.EDIP.search.dto.SearchRequest;
import com.example.EDIP.search.dto.SearchResponse;
import com.example.EDIP.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Search Controller
 * Handles search requests for documents and users based on role-based access control
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;

    /**
     * POST /api/search
     *
     * Execute search with advanced filtering and role-based access control
     *
     * Example request body:
     * {
     *   "keyword": "report",
     *   "type": "ALL",
     *   "page": 0,
     *   "size": 10,
     *   "sortDirection": "DESC",
     *   "statuses": ["PENDING", "COMPLETED"],
     *   "startDate": "2024-01-01T00:00:00",
     *   "endDate": "2024-12-31T23:59:59",
     *   "departmentIds": ["uuid1", "uuid2"],
     *   "roleFilters": ["HEAD", "EMPLOYEE"]
     * }
     *
     * @param request   SearchRequest with all filter criteria
     * @param email     Current user email (from JWT token)
     * @return SearchResponse with filtered documents and users
     */
    @PostMapping
    public SearchResponse search(
            @RequestBody(required = false) SearchRequest request,
            @AuthenticationPrincipal String email
    ) {
        // Use default request if not provided
        if (request == null) {
            request = new SearchRequest();
        }

        return service.search(request, email);
    }

    /**
     * GET /api/search (Legacy endpoint - backward compatibility)
     *
     * Query parameters:
     * - keyword: search term (default: "")
     * - type: ALL | DOCS | USERS (default: ALL)
     * - page: page number (default: 0)
     * - size: page size (default: 10)
     * - sortDirection: ASC | DESC (default: DESC)
     * - status: PENDING | COMPLETED (optional, comma separated)
     * - startDate: ISO date format (optional)
     * - endDate: ISO date format (optional)
     *
     * @param keyword       Search keyword
     * @param type          Search type
     * @param page          Page number
     * @param size          Page size
     * @param sortDirection Sort direction
     * @param email         Current user email
     * @return SearchResponse with results
     */
    @GetMapping
    public SearchResponse searchLegacy(
            @RequestParam(defaultValue = "")     String keyword,
            @RequestParam(defaultValue = "ALL")  String type,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal String email
    ) {
        SearchRequest request = SearchRequest.builder()
                .keyword(keyword)
                .type(type)
                .page(page)
                .size(size)
                .sortDirection(sortDirection)
                .build();

        return service.search(request, email);
    }
}