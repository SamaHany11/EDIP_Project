package com.example.EDIP.document.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SubmitDocumentRequest {




    @NotNull(message = "File is required")
    private List<MultipartFile> files;

    @Column(name = "is_confidential", nullable = false)
    private Boolean confidential;


    private String targetUserEmail;
}