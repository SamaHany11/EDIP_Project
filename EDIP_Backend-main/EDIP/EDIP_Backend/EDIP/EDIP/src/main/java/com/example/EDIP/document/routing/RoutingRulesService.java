package com.example.EDIP.document.routing;

import com.example.EDIP.Auth.model.Department;
import com.example.EDIP.Auth.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingRulesService {

    private final RoutingRuleRepository routingRuleRepository;
    private final DepartmentRepository departmentRepository;


    private String normalize(String value) {
        return value == null
                ? null
                : value.trim()
                .toLowerCase()
                .replace(" ", "_");
    }


    public UUID findDepartmentByName(String departmentName) {

        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new RuntimeException("Department name from AI is null or empty");
        }

        String normalizedInput = normalize(departmentName);

        return departmentRepository
                .findAll()
                .stream()
                .filter(dep -> normalize(dep.getDepartmentName()).equals(normalizedInput))
                .map(Department::getDepartmentId)
                .findFirst()
                .orElse(null);
    }


    public UUID findDepartmentByDocumentType(String documentType, String subType) {

        if (documentType == null || documentType.trim().isEmpty()) {
            throw new RuntimeException("Document type is null or empty");
        }

        String normalizedType = normalize(documentType);
        String normalizedSubType = subType != null ? normalize(subType) : null;


        if (normalizedSubType != null && !normalizedSubType.isBlank()) {
            var ruleWithSubType = routingRuleRepository
                    .findByDocumentTypeAndSubType(normalizedType, normalizedSubType);

            if (ruleWithSubType.isPresent()) {
                return ruleWithSubType.get().getTargetDepartmentId();
            }
        }


        return routingRuleRepository
                .findByDocumentType(normalizedType)
                .map(RoutingRule::getTargetDepartmentId)
                .orElseThrow(() -> {
                    log.error("No routing rule found for type: {}", normalizedType);
                    return new RuntimeException(
                            "No routing rule for document type: " + normalizedType
                    );
                });
    }


    private UUID getDefaultDepartmentId() {
        return departmentRepository
                .findByDepartmentNameIgnoreCase("General")
                .map(Department::getDepartmentId)
                .orElseThrow(() -> new RuntimeException(
                        "Default department 'General' not found"
                ));
    }
}
