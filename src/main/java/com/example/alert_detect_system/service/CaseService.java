package com.example.alert_detect_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.alert_detect_system.Model.CaseModel;
import com.example.alert_detect_system.Model.CaseStatus;
import com.example.alert_detect_system.dto.CaseRequestDto;
import com.example.alert_detect_system.repo.CaseRepository;

/**
 * MINIMAL CaseService - Only handles data persistence and basic validation
 * Business logic (workflows, tasks, approvals) should be handled by the UI
 */
@Service
@Transactional
public class CaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);
    
    @Autowired
    private CaseRepository caseRepository;
    
    @Autowired
    private AuditService auditService;
    
    // Basic validation constants - keep these for data integrity
    private static final List<String> VALID_CASE_TYPES = List.of(
        "FRAUD_DETECTION", "MONEY_LAUNDERING", "SUSPICIOUS_ACTIVITY", "COMPLIANCE_VIOLATION",
        "AML", "FRAUD", "COMPLIANCE", "SANCTIONS", "KYC"
    );
    
    private static final List<String> VALID_PRIORITIES = List.of(
        "LOW", "MEDIUM", "HIGH", "CRITICAL"
    );
    
    private static final List<String> VALID_TYPOLOGIES = List.of(
        "MONEY_LAUNDERING", "TERRORIST_FINANCING", "FRAUD", "SANCTIONS_VIOLATION"
    );
    
    // ========== CORE CRUD OPERATIONS ==========
    
    /**
     * Create a new case (basic data creation only)
     */
    public CaseModel createCase(CaseRequestDto caseRequest, String createdBy) {
        logger.info("Creating new case for user: {}", createdBy);
        
        // Basic validation only
        validateCaseRequest(caseRequest);
        
        // Create case entity
        CaseModel newCase = new CaseModel();
        newCase.setCaseNumber(generateCaseNumber());
        updateCaseFields(newCase, caseRequest);
        newCase.setCreatedBy(createdBy);
        newCase.setStatus(CaseStatus.DRAFT); // Always start as DRAFT - UI decides when to change
        
        CaseModel savedCase = caseRepository.save(newCase);
        
        // Simple audit logging
        auditService.logCaseAction(savedCase.getId(), "CASE_CREATED", createdBy, 
            "Case created with ID: " + savedCase.getId());
        
        logger.info("Case created successfully with ID: {}", savedCase.getId());
        return savedCase;
    }
    
    /**
     * Update an existing case (simple field updates only)
     */
    public CaseModel updateCase(UUID caseId, CaseRequestDto updateRequest, String updatedBy) {
        logger.info("Updating case: {} by user: {}", caseId, updatedBy);
        
        CaseModel existingCase = getCaseById(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case not found with ID: " + caseId));
        
        // Basic validation
        validateCaseRequest(updateRequest);
        
        // Update fields
        updateCaseFields(existingCase, updateRequest);
        existingCase.setUpdatedAt(LocalDateTime.now());
        
        CaseModel savedCase = caseRepository.save(existingCase);
        
        // Simple audit
        auditService.logCaseAction(caseId, "CASE_UPDATED", updatedBy, "Case fields updated");
        
        return savedCase;
    }
    
    /**
     * Update case status (simple status change only)
     */
    public CaseModel updateCaseStatus(UUID caseId, CaseStatus newStatus, String updatedBy) {
        logger.info("Updating case status: {} to {} by user: {}", caseId, newStatus, updatedBy);
        
        CaseModel existingCase = getCaseById(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case not found with ID: " + caseId));
        
        CaseStatus oldStatus = existingCase.getStatus();
        existingCase.setStatus(newStatus);
        existingCase.setUpdatedAt(LocalDateTime.now());
        
        CaseModel savedCase = caseRepository.save(existingCase);
        
        // Audit the status change
        auditService.logCaseStatusChange(caseId, updatedBy, oldStatus.toString(), newStatus.toString());
        
        return savedCase;
    }
    
    /**
     * Consolidated action method for controller compatibility
     * BUT simplified - no complex business logic
     */
    public CaseModel performCaseAction(UUID caseId, String action, CaseRequestDto updateRequest, 
                                     String performedBy, Map<String, Object> params) {
        
        switch (action.toLowerCase()) {
            case "update":
                return updateCase(caseId, updateRequest, performedBy);
            case "complete":
                // Just change status to PENDING_CASE_CREATION_APPROVAL - UI handles the rest
                return updateCaseStatus(caseId, CaseStatus.PENDING_CASE_CREATION_APPROVAL, performedBy);
            case "approve":
                // Just change status based on approval - UI handles workflow logic
                boolean approved = (Boolean) params.getOrDefault("approved", false);
                CaseStatus newStatus = approved ? CaseStatus.READY_FOR_ASSIGNMENT : CaseStatus.DRAFT;
                return updateCaseStatus(caseId, newStatus, performedBy);
            case "status":
                CaseStatus status = (CaseStatus) params.get("status");
                return updateCaseStatus(caseId, status, performedBy);
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }
    }
    
    // ========== QUERY METHODS ==========
    
    public Optional<CaseModel> getCaseById(UUID caseId) {
        return caseRepository.findById(caseId);
    }
    
    public List<CaseModel> getAllCases() {
        return caseRepository.findAll();
    }
    
    public List<CaseModel> getCasesByStatus(CaseStatus status) {
        return caseRepository.findByStatus(status);
    }
    
    public List<CaseModel> getCasesByCreatedBy(String createdBy) {
        return caseRepository.findByCreatedBy(createdBy);
    }
    
    /**
     * Get recent cases ordered by creation date (newest first)
     * Used by both admin and analyst dashboards
     */
    public List<CaseModel> getRecentCases(int limit) {
        return caseRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .limit(limit)
            .toList();
    }
    
    /**
     * Get recent cases for a specific user
     */
    public List<CaseModel> getRecentCasesByUser(String createdBy, int limit) {
        return caseRepository.findByCreatedByOrderByCreatedAtDesc(createdBy)
            .stream()
            .limit(limit)
            .toList();
    }

    /**
     * Delete a case (with validation)
     * Both admin and analyst can delete their own cases
     */
    public void deleteCase(UUID caseId, String deletedBy) {
        logger.info("Deleting case: {} by user: {}", caseId, deletedBy);
        
        CaseModel existingCase = getCaseById(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case not found with ID: " + caseId));
        
        // Business rule: Only allow deletion of DRAFT cases or cases created by the user
        if (existingCase.getStatus() != CaseStatus.DRAFT && !existingCase.getCreatedBy().equals(deletedBy)) {
            throw new IllegalArgumentException("Can only delete DRAFT cases or cases you created");
        }
        
        // Audit before deletion
        auditService.logCaseAction(caseId, "CASE_DELETED", deletedBy, 
            "Case deleted: " + existingCase.getCaseNumber());
        
        caseRepository.delete(existingCase);
        
        logger.info("Case deleted successfully: {}", caseId);
    }

    // ========== PRIVATE HELPER METHODS (Simple Data Operations Only) ==========
    
    private void updateCaseFields(CaseModel caseModel, CaseRequestDto updateRequest) {
        if (updateRequest.getCaseType() != null) caseModel.setCaseType(updateRequest.getCaseType());
        if (updateRequest.getPriority() != null) caseModel.setPriority(updateRequest.getPriority());
        if (updateRequest.getDescription() != null) caseModel.setDescription(updateRequest.getDescription());
        if (updateRequest.getEntity() != null) caseModel.setEntity(updateRequest.getEntity());
        if (updateRequest.getAlertId() != null) caseModel.setAlertId(updateRequest.getAlertId());
        if (updateRequest.getRiskScore() != null) caseModel.setRiskScore(updateRequest.getRiskScore());
        if (updateRequest.getTypology() != null) caseModel.setTypology(updateRequest.getTypology());
    }
    
    private void validateCaseRequest(CaseRequestDto caseRequest) {
        if (caseRequest.getCaseType() != null && !VALID_CASE_TYPES.contains(caseRequest.getCaseType())) {
            throw new IllegalArgumentException("Invalid case type: " + caseRequest.getCaseType());
        }
        if (caseRequest.getPriority() != null && !VALID_PRIORITIES.contains(caseRequest.getPriority())) {
            throw new IllegalArgumentException("Invalid priority: " + caseRequest.getPriority());
        }
        if (caseRequest.getTypology() != null && !VALID_TYPOLOGIES.contains(caseRequest.getTypology())) {
            throw new IllegalArgumentException("Invalid typology: " + caseRequest.getTypology());
        }
        if (caseRequest.getRiskScore() != null && 
            (caseRequest.getRiskScore() < 0 || caseRequest.getRiskScore() > 100)) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100");
        }
    }
    
    private String generateCaseNumber() {
        int year = LocalDateTime.now().getYear();
        long count = caseRepository.count() + 1;
        return String.format("CASE-%d-%04d", year, count);
    }
}