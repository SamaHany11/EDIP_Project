package com.example.EDIP.template.repository;

import com.example.EDIP.template.model.TemplateSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TemplateSubmissionRepository extends JpaRepository<TemplateSubmission, UUID> {


}
