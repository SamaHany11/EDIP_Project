package com.example.EDIP.document.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
public class ForwardToDepartmentRequest {

    @NotNull(message = "Department email is required")
    private String departmentEmail;

    private String notes;

    private MultipartFile finalSignedFile;
}