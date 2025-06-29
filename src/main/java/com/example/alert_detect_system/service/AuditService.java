package com.example.alert_detect_system.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.alert_detect_system.Model.AuditLogModel;
import com.example.alert_detect_system.repo.AuditLogRepo;

@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    @Autowired
    private AuditLogRepo auditLogRepository;
    
    public void logCaseAction(UUID caseId, String action, String performedBy, String details) {
        try {
            AuditLogModel auditLog = new AuditLogModel(caseId, action, performedBy, details);
            auditLogRepository.save(auditLog);
            logger.info("Audit log created - Case: {}, Action: {}, User: {}", caseId, action, performedBy);
        } catch (Exception e) {
            logger.error("Failed to create audit log for case: {}", caseId, e);
        }
    }
    
    public void logCaseStatusChange(UUID caseId, String performedBy, String oldStatus, String newStatus) {
        AuditLogModel auditLog = new AuditLogModel(caseId, "STATUS_CHANGE", performedBy,
            String.format("Status changed from %s to %s", oldStatus, newStatus));
        auditLog.setOldValue(oldStatus);
        auditLog.setNewValue(newStatus);
        auditLogRepository.save(auditLog);
    }
    
    public List<AuditLogModel> getCaseAuditLogs(UUID caseId) {
        return auditLogRepository.findByCaseIdOrderByTimestampDesc(caseId);
    }
    
    // Log task-level actions (for assignment/reassignment)
    public void logTaskAction(String taskId, String action, String performedBy, String details) {
        try {
            AuditLogModel auditLog = new AuditLogModel(null, action, performedBy, details);
            auditLog.setTaskId(taskId);
            auditLogRepository.save(auditLog);
            logger.info("Audit log created - Task: {}, Action: {}, User: {}", taskId, action, performedBy);
        } catch (Exception e) {
            logger.error("Failed to create audit log for task: {}", taskId, e);
        }
    }
}