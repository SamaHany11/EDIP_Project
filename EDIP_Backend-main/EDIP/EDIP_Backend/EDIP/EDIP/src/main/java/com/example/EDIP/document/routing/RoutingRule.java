package com.example.EDIP.document.routing;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Routing_Rules", indexes = {
        @Index(name = "IDX_Routing_Type", columnList = "document_type, sub_type")
})
public class RoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "sub_type")
    private String subType;

    @Column(name = "target_department_id", nullable = false)
    private UUID targetDepartmentId;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}