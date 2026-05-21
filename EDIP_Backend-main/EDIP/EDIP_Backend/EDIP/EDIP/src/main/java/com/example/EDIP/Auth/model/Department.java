package com.example.EDIP.Auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "Departments")
public class Department {

    @Id
    @GeneratedValue
    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "department_name", nullable = false)
    private String departmentName;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
