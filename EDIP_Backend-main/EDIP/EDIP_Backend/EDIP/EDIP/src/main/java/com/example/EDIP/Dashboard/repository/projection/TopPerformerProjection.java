package com.example.EDIP.Dashboard.repository.projection;

import java.util.UUID;

public interface TopPerformerProjection {
    UUID   getDepartmentId();
    String getUsername();

    Long   getCompletedCount();
}