package com.example.EDIP.template.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "template_submissions")
@Data
public class TemplateSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_type")
    private String templateType;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "assigned_to_head_id")
    private UUID assignedToHeadId;

    private String status;

    @Column(name = "payload", columnDefinition = "NVARCHAR(MAX)")
    private String payload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;


}