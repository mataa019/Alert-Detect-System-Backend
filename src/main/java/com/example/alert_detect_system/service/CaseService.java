package com.example.alert_detect_system.service;

import com.example.alert_detect_system.Model.CaseModel;
import com.example.alert_detect_system.Model.CaseStatus;
import com.example.alert_detect_system.dto.CaseRequestDto;
import com.example.alert_detect_system.repo.CaseRepository;
import com.example.alert_detect_system.workflow.CaseWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);
    
    @Autowired
    private CaseRepository caseRepository;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private CaseWorkflowService caseWorkflowService;
      private static final List<String> VALID_CASE_TYPES = List.of(
        "FRAUD_DETECTION", "MONEY_LAUNDERING", "SUSPICIOUS_ACTIVITY", "COMPLIANCE_VIOLATION",
        "AML", "FRAUD", "COMPLIANCE", "SANCTIONS", "KYC"  // Keep old values for backward compatibility
    );
    
    private static final List<String> VALID_PRIORITIES = List.of(
        "LOW", "MEDIUM", "HIGH", "CRITICAL"
    );
    
    private static final List<String> VALID_TYPOLOGIES = List.of(
        "MONEY_LAUNDERING", "TERRORIST_FINANCING", "FRAUD", "SANCTIONS_VIOLATION"
    );
    
    public CaseModel createCase(CaseRequestDto caseRequest, String createdBy) {
        logger.info("Creating new case for user: {}", createdBy);
        
        // Validate input
        validateCaseRequest(caseRequest);
        
        // Create case entity
        CaseModel newCase = new CaseModel();
        newCase.setCaseNumber(generateCaseNumber());
        newCase.setCaseType(caseRequest.getCaseType());
        newCase.setPriority(caseRequest.getPriority());
        newCase.setEntity(caseRequest.getEntity());
        newCase.setAlertId(caseRequest.getAlertId());
        newCase.setDescription(caseRequest.getDescription());
        newCase.setRiskScore(caseRequest.getRiskScore());
        newCase.setTypology(caseRequest.getTypology());
        newCase.setCreatedBy(createdBy);
        
        // Determine initial status
        if (isCompleteCase(caseRequest)) {
            newCase.setStatus(CaseStatus.READY_FOR_ASSIGNMENT);
        } else {
            newCase.setStatus(CaseStatus.DRAFT);
        }
        
        // Save case
        CaseModel savedCase = caseRepository.save(newCase);
        
        // Log audit
        auditService.logCaseAction(savedCase.getId(), "CASE_CREATED", createdBy, 
            String.format("Case created with type: %s, priority: %s", 
                savedCase.getCaseType(), savedCase.getPriority()));
        
        // Start workflow if case is ready
        if (savedCase.getStatus() == CaseStatus.READY_FOR_ASSIGNMENT) {
            try {
                String processInstanceId = caseWorkflowService.startCaseWorkflow(savedCase);
                savedCase.setProcessInstanceId(processInstanceId);
                savedCase = caseRepository.save(savedCase);
                
                auditService.logCaseAction(savedCase.getId(), "WORKFLOW_STARTED", createdBy, 
                    "BPMN workflow process started");
            } catch (Exception e) {
                logger.error("Failed to start workflow for case: {}", savedCase.getId(), e);
                // Case is still saved, workflow can be started manually later
            }
        }
        
        logger.info("Case created successfully with ID: {} and number: {}", 
            savedCase.getId(), savedCase.getCaseNumber());
        
        return savedCase;
    }
    
    public CaseModel updateCaseStatus(UUID caseId, CaseStatus newStatus, String updatedBy) {
        Optional<CaseModel> caseOpt = caseRepository.findById(caseId);
        if (caseOpt.isEmpty()) {
            throw new IllegalArgumentException("Case not found with ID: " + caseId);
        }
        
        CaseModel existingCase = caseOpt.get();
        CaseStatus oldStatus = existingCase.getStatus();
        
        existingCase.setStatus(newStatus);
        existingCase.setUpdatedAt(LocalDateTime.now());
        
        CaseModel updatedCase = caseRepository.save(existingCase);
        
        // Log status change
        auditService.logCaseStatusChange(caseId, updatedBy, oldStatus.toString(), newStatus.toString());
          return updatedCase;
    }
    
    public CaseModel updateCase(UUID caseId, CaseRequestDto caseUpdate, String updatedBy) {
        logger.info("Updating case with ID: {} by user: {}", caseId, updatedBy);
        
        // Get existing case
        Optional<CaseModel> caseOpt = caseRepository.findById(caseId);
        if (caseOpt.isEmpty()) {
            throw new IllegalArgumentException("Case not found with ID: " + caseId);
        }
        
        CaseModel existingCase = caseOpt.get();
        
        // Validate that case is in DRAFT status
        if (!CaseStatus.DRAFT.equals(existingCase.getStatus())) {
            throw new IllegalArgumentException("Only DRAFT cases can be updated");
        }
        
        // Validate updated data
        validateCaseRequest(caseUpdate);
        
        // Update case fields
        if (caseUpdate.getCaseType() != null) {
            existingCase.setCaseType(caseUpdate.getCaseType());
        }
        if (caseUpdate.getPriority() != null) {
            existingCase.setPriority(caseUpdate.getPriority());
        }
        if (caseUpdate.getDescription() != null) {
            existingCase.setDescription(caseUpdate.getDescription());
        }        if (caseUpdate.getRiskScore() != null) {
            existingCase.setRiskScore(caseUpdate.getRiskScore());
        }
        // Note: CustomerDetails not available in CaseRequestDto, would need separate DTO for updates
        
        existingCase.setUpdatedAt(LocalDateTime.now());
        
        // Check if case is now complete and should be moved to PENDING_CASE_CREATION_APPROVAL
        if (isCaseComplete(existingCase)) {
            existingCase.setStatus(CaseStatus.PENDING_CASE_CREATION_APPROVAL);
            auditService.logCaseStatusChange(caseId, updatedBy, "DRAFT", "PENDING_CASE_CREATION_APPROVAL");
        }
        
        CaseModel savedCase = caseRepository.save(existingCase);
        
        // Log case update
        auditService.logCaseAction(caseId, "CASE_UPDATED", updatedBy, 
            "Case details updated and validation completed");
        
        logger.info("Case updated successfully: {}", caseId);
        return savedCase;
    }
    
    private boolean isCaseComplete(CaseModel caseModel) {
        // Check if all mandatory fields are filled
        return caseModel.getDescription() != null && !caseModel.getDescription().trim().isEmpty()
            && caseModel.getCaseType() != null && !caseModel.getCaseType().trim().isEmpty()
            && caseModel.getPriority() != null && !caseModel.getPriority().trim().isEmpty()
            && caseModel.getRiskScore() != null && caseModel.getRiskScore() > 0;
    }
    
    public List<CaseModel> getAllCases() {
        return caseRepository.findAll();
    }
    
    public Optional<CaseModel> getCaseById(UUID caseId) {
        return caseRepository.findById(caseId);
    }
    
    public Optional<CaseModel> getCaseByCaseNumber(String caseNumber) {
        return caseRepository.findByCaseNumber(caseNumber);
    }
    
    public List<CaseModel> getCasesByStatus(CaseStatus status) {
        return caseRepository.findByStatus(status);
    }
    
    public List<CaseModel> getCasesByCreatedBy(String createdBy) {
        return caseRepository.findByCreatedBy(createdBy);
    }
      private void validateCaseRequest(CaseRequestDto caseRequest) {
        // Validate case type
        if (caseRequest.getCaseType() != null && !VALID_CASE_TYPES.contains(caseRequest.getCaseType())) {
            throw new IllegalArgumentException("Invalid case type: " + caseRequest.getCaseType());
        }
        
        // Validate priority
        if (caseRequest.getPriority() != null && !VALID_PRIORITIES.contains(caseRequest.getPriority())) {
            throw new IllegalArgumentException("Invalid priority: " + caseRequest.getPriority());
        }
        
        // Validate typology (optional field)
        if (caseRequest.getTypology() != null && !VALID_TYPOLOGIES.contains(caseRequest.getTypology())) {
            throw new IllegalArgumentException("Invalid typology: " + caseRequest.getTypology());
        }
        
        // Validate risk score
        if (caseRequest.getRiskScore() != null && 
            (caseRequest.getRiskScore() < 0 || caseRequest.getRiskScore() > 100)) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100");
        }
    }
    
    private boolean isCompleteCase(CaseRequestDto caseRequest) {
        return caseRequest.getCaseType() != null &&
               caseRequest.getPriority() != null &&
               caseRequest.getEntity() != null &&
               caseRequest.getAlertId() != null &&
               caseRequest.getTypology() != null;
    }
    
    private String generateCaseNumber() {
        // Generate format: CASE-YYYY-NNNN
        int year = LocalDateTime.now().getYear();
        long count = caseRepository.count() + 1;
        return String.format("CASE-%d-%04d", year, count);
    }
}