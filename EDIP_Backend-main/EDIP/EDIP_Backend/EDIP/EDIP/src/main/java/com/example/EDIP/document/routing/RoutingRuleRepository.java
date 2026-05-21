package com.example.EDIP.document.routing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository
        extends JpaRepository<RoutingRule, UUID> {

    Optional<RoutingRule> findByDocumentType(String documentType);

    Optional<RoutingRule> findByDocumentTypeAndSubType(
            String documentType, String subType);
}