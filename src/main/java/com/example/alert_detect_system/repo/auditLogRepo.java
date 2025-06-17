package com.example.alert_detect_system.repo;

import com.example.alert_detect_system.Model.AuditLogModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepo extends JpaRepository<AuditLogModel, UUID> {
    List<AuditLogModel> findByCaseIdOrderByTimestampDesc(UUID caseId);
    
    List<AuditLogModel> findByPerformedBy(String performedBy);

}
