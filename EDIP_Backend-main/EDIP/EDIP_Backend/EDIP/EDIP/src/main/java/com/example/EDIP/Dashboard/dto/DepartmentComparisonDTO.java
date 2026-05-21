package com.example.EDIP.Dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class DepartmentComparisonDTO {

    private String         generatedAt;
    private String         range;
    private String         sortOrder;
    private int            totalDepartments;
    private List<DeptStat> departments;

    @Data @Builder
    public static class DeptStat {
        private UUID   departmentId;
        private String departmentName;
        private long   totalDocuments;
        private long   submitted;
        private long   inProgress;
        private long   pending;
        private long   completed;
        private long   totalEmployees;
        private String topPerformer;
    }
}