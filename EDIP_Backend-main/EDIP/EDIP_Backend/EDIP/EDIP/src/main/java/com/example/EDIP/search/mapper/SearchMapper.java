package com.example.EDIP.search.mapper;

import com.example.EDIP.Auth.model.Department;
import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.DepartmentRepository;
import com.example.EDIP.document.model.sql.Document;
import com.example.EDIP.search.dto.DocumentDTO;
import com.example.EDIP.search.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchMapper {

    private final DepartmentRepository departmentRepository;

    public DocumentDTO toDocumentDTO(Document d) {
        return new DocumentDTO(
                d.getDocumentId(),
                d.getFileName(),
                d.getStatus().name(),
                d.getDepartmentName(),
                d.getSubmittedDate()
        );
    }

    public DocumentDTO toDocumentDTOExternal(Document d) {
        return new DocumentDTO(
                d.getDocumentId(),
                d.getFileName(),
                d.getStatus().name(),
                null,              // departmentName not exposed to External users
                d.getSubmittedDate()
        );
    }

    public UserDTO toUserDTO(User u) {
        String deptName = null;
        if (u.getDepartmentId() != null) {
            deptName = departmentRepository.findById(u.getDepartmentId())
                    .map(Department::getDepartmentName)
                    .orElse(null);
        }
        return new UserDTO(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                deptName
        );
    }
}