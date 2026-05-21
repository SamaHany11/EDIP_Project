package com.example.EDIP.Dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class EmployeeDashboardDTO {

    private UUID          userId;
    private String        username;
    private String        departmentName;
    private String        generatedAt;
    private String        range;

    private DocumentStats documents;

    @Data @Builder
    public static class DocumentStats {
        private long total;
        private long submitted;
        private long inProgress;
        private long pending;
        private long completed;
    }
}