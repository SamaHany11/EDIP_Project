package com.example.EDIP.document.model.sql;

import java.util.Map;

public class ActionLabels {
    public static final Map<String, String> LABELS = Map.of(
            "DOCUMENT_SUBMITTED", "Document Submitted",
            "ASSIGNED_TO_EMPLOYEE", "Assigned to Employee",
            "FORWARDED_TO_DEPARTMENT", "Forwarded to Department",
            "FORWARDED_TO_COLLEAGUE", "Forwarded to Colleague",
            "REPLY_CREATED", "Reply Created",
            "REPLY_APPROVED", "Reply Approved",
            "REPLY_REJECTED", "Reply Rejected",
            "DOCUMENT_SENT_TO_CLIENT", "Sent to Client"

    );

    public static String label(String actionType) {
        return LABELS.getOrDefault(actionType, actionType);
    }
}
