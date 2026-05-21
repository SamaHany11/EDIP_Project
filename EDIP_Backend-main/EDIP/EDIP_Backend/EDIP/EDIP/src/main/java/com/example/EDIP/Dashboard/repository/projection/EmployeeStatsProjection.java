package com.example.EDIP.Dashboard.repository.projection;

import java.util.UUID;

public interface EmployeeStatsProjection {
    UUID   getUserId();
    String getUsername();
    String getEmail();
    Long   getAssignedDocuments();
    Long   getCompletedDocuments();
    Long   getPendingDocuments();
    Long   getInProgressDocuments();
}