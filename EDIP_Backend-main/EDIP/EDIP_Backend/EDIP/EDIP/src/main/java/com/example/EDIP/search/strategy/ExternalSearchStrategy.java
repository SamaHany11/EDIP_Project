package com.example.EDIP.search.strategy;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.search.dto.*;
import com.example.EDIP.search.mapper.SearchMapper;
import com.example.EDIP.search.repository.DocumentSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * External User Search Strategy
 * External users can see ONLY documents they uploaded
 */
@Service("externalSearchStrategy")
@RequiredArgsConstructor
public class ExternalSearchStrategy extends AbstractSearchStrategy implements SearchStrategy {

    private final DocumentSearchRepository docRepo;
    private final SearchMapper             mapper;

    @Override
    public SearchResponse search(
            String keyword,
            int page,
            int size,
            String sortDirection,
            String type,
            User user,
            SearchRequest request
    ) {
        ResultWrapper<DocumentDTO> docs  = null;
        ResultWrapper<UserDTO>     users = null;

        // ========== DOCUMENTS SEARCH ==========
        if ("ALL".equalsIgnoreCase(type) || "DOCS".equalsIgnoreCase(type)) {
            Pageable dp = buildDocumentPageable(page, size, sortDirection);


            Page<Document> docPage = docRepo.findDocumentsForExternal(
                    user.getId(),
                    getSafeKeyword(keyword),
                    request.getStatuses(),
                    request.getStartDate(),
                    request.getEndDate(),
                    dp
            );

            docs = wrap(docPage, mapper::toDocumentDTOExternal);
        }

        return new SearchResponse(docs, users);
    }
}