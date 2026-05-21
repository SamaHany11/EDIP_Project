package com.example.EDIP.document.repository.sql;

import com.example.EDIP.document.model.sql.AutoLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AutoLogRepository extends JpaRepository<AutoLog, UUID> {

    List<AutoLog> findByDocumentIdOrderByActionTimestampAsc(UUID documentId);

    Page<AutoLog> findByUserIdOrderByActionTimestampDesc(
            UUID userId, Pageable pageable);


    Page<AutoLog> findByDepartmentIdOrderByActionTimestampDesc(UUID departmentId, Pageable pageable);

    Page<AutoLog> findAllByOrderByActionTimestampDesc(Pageable pageable);

    @Query("SELECT DISTINCT a.documentId FROM AutoLog a")
    List<UUID> findDistinctDocumentIds();

    long countByDocumentId(UUID documentId);

    AutoLog findTopByDocumentIdOrderByActionTimestampDesc(UUID documentId);

    @Query("SELECT DISTINCT a.documentId FROM AutoLog a WHERE a.userId = :userId")
    List<UUID> findDistinctDocumentIdsByUserId(UUID userId);

    @Query("SELECT DISTINCT a.documentId FROM AutoLog a WHERE a.departmentId = :departmentId")
    List<UUID> findDistinctDocumentIdsByDepartmentId(UUID departmentId);


}