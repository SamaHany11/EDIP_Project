package com.example.EDIP.Dashboard.repository.projection;

import java.util.UUID;

public interface DeptDocStatsProjection {
    UUID getDepartmentId();
    Long getTotalDocuments();
    Long getSubmitted();
    Long getInProgress();
    Long getPending();
    Long getCompleted();
}