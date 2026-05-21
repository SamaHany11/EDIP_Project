package com.example.EDIP.document.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter
@Setter
public class CompleteDocumentRequest {
    private List<MultipartFile> finalFiles;
    private String notes;
}