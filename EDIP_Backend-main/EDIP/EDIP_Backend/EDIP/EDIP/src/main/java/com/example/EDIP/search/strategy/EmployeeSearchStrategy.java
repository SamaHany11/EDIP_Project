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
 * Employee User Search Strategy
 * Employees can see:
 * - Documents they uploaded
 * - Documents assigned to them
 * - Users in their same department
 *
 * docScope values:
 * - null / not sent → uploaded by me + assigned to me (default)
 * - UPLOADED_BY_ME  → only docs the employee uploaded
 * - ASSIGNED_TO_ME  → only docs assigned to the employee
 */
@Service("employeeSearchStrategy")
@RequiredArgsConstructor
public class EmployeeSearchStrategy extends AbstractSearchStrategy implements SearchStrategy {

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

                docPage = docRepo.findDocumentsUploadedByEmployee(
                        user.getId(),
                        getSafeKeyword(keyword),
                        request.getStatuses(),
                        request.getStartDate(),
                        request.getEndDate(),
                        dp
                );

            } else if ("ASSIGNED_TO_ME".equalsIgnoreCase(scope)) {

                docPage = docRepo.findDocumentsAssignedToEmployee(
                        user.getId(),
                        getSafeKeyword(keyword),
                        request.getStatuses(),
                        request.getStartDate(),
                        request.getEndDate(),
                        dp
                );

            } else {
                // Default: uploaded by me + assigned to me
                docPage = docRepo.findDocumentsForEmployee(
                        user.getId(),
                        getSafeKeyword(keyword),
                        request.getStatuses(),
                        request.getStartDate(),
                        request.getEndDate(),
                        dp
                );
            }

            docs = wrap(docPage, mapper::toDocumentDTO);
        }

        // ========== USERS SEARCH ==========

        if ("ALL".equalsIgnoreCase(type) || "USERS".equalsIgnoreCase(type)) {
            Pageable up = buildUserPageable(page, size, sortDirection);

            Page<User> userPage = userRepo.findUsersForEmployee(
                    user.getDepartmentId(),
                    getSafeKeyword(keyword),
                    user.getId(),
                    up
            );

            users = wrap(userPage, mapper::toUserDTO);
        }

        return new SearchResponse(docs, users);
    }
}