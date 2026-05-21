package com.example.EDIP.search.strategy;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.search.dto.SearchRequest;
import com.example.EDIP.search.dto.SearchResponse;

public interface SearchStrategy {

    /**
     * Execute search based on user role and criteria
     */
    SearchResponse search(
            String keyword,
            int page,
            int size,
            String sortDirection,
            String type,
            User user,
            SearchRequest request
    );
}