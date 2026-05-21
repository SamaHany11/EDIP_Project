package com.example.EDIP.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDTO {
    private UUID id;
    private String fileName;
    private String status;
    private String departmentName;
    private LocalDateTime submittedDate;
}