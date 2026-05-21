package com.example.EDIP.Dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class AdminDashboardDTO {

    private UUID   departmentId;
    private String departmentName;
    private String generatedAt;
    private String range;
    private String sortOrder;

    private DocumentStats  documents;
    private EmployeeGroup  employees;

    @Data @Builder
    public static class DocumentStats {
        private long total;
        private long submitted;
        private long inProgress;
        private long pending;
        private long completed;
    }

    @Data @Builder
    public static class EmployeeGroup {
        private long               total;
        private List<EmployeeStat> stats;
    }

    @Data @Builder
    public static class EmployeeStat {
        private UUID             userId;
        private String           username;
        private String           email;
        private EmployeeDocStats documents;
    }

    @Data @Builder
    public static class EmployeeDocStats {
        private long assigned;
        private long completed;
        private long pending;
        private long inProgress;
    }
}