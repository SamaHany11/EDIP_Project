package com.example.EDIP.search.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.search.dto.SearchRequest;
import com.example.EDIP.search.dto.SearchResponse;
import com.example.EDIP.search.strategy.SearchStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Search Service
 * Orchestrates the search workflow:
 * 1. Get current user from database
 * 2. Route to appropriate strategy based on role
 * 3. Pass search request with all filters
 * 4. Return filtered results
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SearchStrategyFactory factory;
    private final UserRepository        userRepository;

    /**
     * Execute search based on user role and request criteria
     *
     * @param request SearchRequest containing all filter criteria
     * @param email   Current user email from JWT token
     * @return SearchResponse with filtered documents and users
     */
    public SearchResponse search(SearchRequest request, String email) {
        // Validate input
        if (request == null) {
            request = new SearchRequest();
        }

        // Get current user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        log.debug("Search request from user: {} (role: {})", user.getUsername(), user.getRole());
        log.debug("Search criteria - keyword: {}, type: {}, statuses: {}",
                request.getKeyword(), request.getType(), request.getStatuses());

        // Route to appropriate strategy based on user role
        var strategy = factory.get(user);

        // Execute search with full request object
        return strategy.search(
                request.getKeyword(),
                request.getPage(),
                request.getSize(),
                request.getSortDirection(),
                request.getType(),
                user,
                request // Pass entire request for filter access
        );
    }

    /**
     * Legacy method for backward compatibility
     * Converts old method signature to new one
     */
    public SearchResponse search(
            String keyword,
            int page,
            int size,
            String sortDirection,
            String type,
            String email
    ) {
        SearchRequest request = SearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortDirection(sortDirection)
                .type(type)
                .build();

        return search(request, email);
    }
}