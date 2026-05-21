package com.example.EDIP.search.strategy;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.search.dto.*;
import com.example.EDIP.search.mapper.SearchMapper;
import com.example.EDIP.search.repository.DocumentSearchRepository;
import com.example.EDIP.search.repository.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Admin User Search Strategy
 * Admins can see:
 * - ALL documents with optional filters (status, date range, departments, departmentName)
 * - ALL users with optional filters (role, department, departmentName)
 *
 * docScope values:
 * - null / not sent → all documents (default)
 * - UPLOADED_BY_ME  → only docs the admin uploaded
 * - BY_DEPARTMENT   → docs filtered by departmentIds / departmentName
 */
@Service("adminSearchStrategy")
@RequiredArgsConstructor
public class AdminSearchStrategy extends AbstractSearchStrategy implements SearchStrategy {

    private final DocumentSearchRepository docRepo;
    private final UserSearchRepository     userRepo;
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
            Page<Document> docPage;

            String scope = request.getDocScope();

            if ("UPLOADED_BY_ME".equalsIgnoreCase(scope)) {

                docPage = docRepo.findDocumentsUploadedByAdmin(
                        user.getId(),
                        getSafeKeyword(keyword),
                        request.getStatuses(),
                        request.getStartDate(),
                        request.getEndDate(),
                        dp
                );

            } else {

                docPage = docRepo.findDocumentsForAdmin(
                        getSafeKeyword(keyword),
                        request.getStatuses(),
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getDepartmentIds(),
                        request.getDepartmentName(),
                        dp
                );
            }

            docs = wrap(docPage, mapper::toDocumentDTO);
        }

        // ========== USERS SEARCH ==========
        if ("ALL".equalsIgnoreCase(type) || "USERS".equalsIgnoreCase(type)) {
            Pageable up = buildUserPageable(page, size, sortDirection);

            Page<User> userPage = userRepo.findUsersForAdmin(
                    getSafeKeyword(keyword),
                    request.getRoleFilters(),
                    request.getDepartmentIds(),
                    request.getDepartmentName(),
                    up
            );

            users = wrap(userPage, mapper::toUserDTO);
        }

        return new SearchResponse(docs, users);
    }
}