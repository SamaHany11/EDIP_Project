package com.example.EDIP.template.Service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.template.model.TemplateSubmission;
import com.example.EDIP.template.repository.TemplateSubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TemplateService {



    private final UserRepository userRepository;
    private final TemplateSubmissionRepository templateSubmissionRepository;
    private final ObjectMapper objectMapper;

    public void submitTemplate(String email, String type, Object dto) {

        // 1 Get user from token email
        User employee = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        // 2️ Department
        var departmentId = employee.getDepartmentId();

        // 3️ Head of department
        User head = userRepository.findHeadByDepartmentId(departmentId)
                .orElseThrow(() -> new RuntimeException("HEAD_NOT_FOUND"));

        // 4️ Save submission
        TemplateSubmission submission = new TemplateSubmission();
        submission.setTemplateType(type);
        submission.setEmployeeId(employee.getId()); // internal only
        submission.setDepartmentId(departmentId);
        submission.setAssignedToHeadId(head.getId());
        submission.setStatus("PENDING");
        submission.setPayload(toJson(dto));
        submission.setCreatedAt(LocalDateTime.now());

        templateSubmissionRepository.save(submission);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON_ERROR");
        }
    }
}