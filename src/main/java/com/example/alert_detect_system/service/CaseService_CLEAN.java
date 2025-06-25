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
import com.example.alert_detect_system.workflow.CaseWorkflowService;

@Service
@Transactional
public class CaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);
    
    @Autowired
    private CaseRepository caseRepository;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private CaseWorkflowService caseWorkflowService;
    
    // Validation constants
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
    
    // ========== CORE METHODS (Actually Used) ==========
    
    /**
     * Create a new case
     */
    public CaseModel createCase(CaseRequestDto caseRequest, String createdBy) {
        logger.info("Creating new case for user: {}", createdBy);
        
        validateCaseRequest(caseRequest);
        
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
        
        // Set initial status based on completeness
        newCase.setStatus(isCompleteCase(caseRequest) ? CaseStatus.READY_FOR_ASSIGNMENT : CaseStatus.DRAFT);
        
        CaseModel savedCase = caseRepository.save(newCase);
        
        // Audit logging
        auditService.logCaseAction(savedCase.getId(), "CASE_CREATED", createdBy, 
            String.format("Case created with type: %s, priority: %s, status: %s", 
                savedCase.getCaseType(), savedCase.getPriority(), savedCase.getStatus()));
        
        // Create initial task
        createInitialTask(savedCase, createdBy);
        
        logger.info("Case created successfully with ID: {}", savedCase.getId());
        return savedCase;
    }
    
    /**
     * Consolidated method for all case actions: update, complete, approve, status
     */
    public CaseModel performCaseAction(UUID caseId, String action, CaseRequestDto updateRequest, 
                                     String performedBy, Map<String, Object> params) {
        logger.info("Performing action '{}' on case: {} by user: {}", action, caseId, performedBy);
        
        CaseModel existingCase = getCaseById(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case not found with ID: " + caseId));
        
        switch (action.toLowerCase()) {
            case "update":
                return handleUpdate(existingCase, updateRequest, performedBy);
            case "complete":
                return handleComplete(existingCase, updateRequest, performedBy);
            case "approve":
                boolean approved = (Boolean) params.getOrDefault("approved", false);
                String comments = (String) params.getOrDefault("comments", "");
                return handleApproval(existingCase, approved, comments, performedBy);
            case "status":
                CaseStatus newStatus = (CaseStatus) params.get("status");
                return handleStatusUpdate(existingCase, newStatus, performedBy);
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
    
    // ========== PRIVATE HELPER METHODS ==========
    
    private CaseModel handleUpdate(CaseModel existingCase, CaseRequestDto updateRequest, String updatedBy) {
        if (!CaseStatus.DRAFT.equals(existingCase.getStatus())) {
            throw new IllegalArgumentException("Only DRAFT cases can be updated");
        }
        
        validateCaseRequest(updateRequest);
        updateCaseFields(existingCase, updateRequest);
        existingCase.setUpdatedAt(LocalDateTime.now());
        
        CaseModel savedCase = caseRepository.save(existingCase);
        auditService.logCaseAction(existingCase.getId(), "CASE_UPDATED", updatedBy, "Case details updated");
        
        return savedCase;
    }
    
    private CaseModel handleComplete(CaseModel existingCase, CaseRequestDto updateRequest, String updatedBy) {
        if (existingCase.getStatus() != CaseStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT cases can be completed");
        }
        
        updateCaseFields(existingCase, updateRequest);
        
        // Determine if approval is needed
        boolean requiresApproval = requiresApproval(existingCase);
        existingCase.setStatus(requiresApproval ? CaseStatus.PENDING_CASE_CREATION_APPROVAL : CaseStatus.READY_FOR_ASSIGNMENT);
        existingCase.setUpdatedBy(updatedBy);
        existingCase.setUpdatedAt(LocalDateTime.now());
        
        CaseModel savedCase = caseRepository.save(existingCase);
        
        auditService.logCaseAction(savedCase.getId(), "CASE_COMPLETED", updatedBy, 
            "Case completed with status: " + savedCase.getStatus());
        
        // Create appropriate next task
        if (requiresApproval) {
            createApprovalTask(savedCase, updatedBy);
        } else {
            startWorkflow(savedCase, updatedBy);
        }
        
        return savedCase;
    }
    
    private CaseModel handleApproval(CaseModel existingCase, boolean approved, String comments, String approvedBy) {
        if (existingCase.getStatus() != CaseStatus.PENDING_CASE_CREATION_APPROVAL) {
            throw new IllegalStateException("Only pending approval cases can be approved");
        }
        
        if (approved) {
            existingCase.setStatus(CaseStatus.READY_FOR_ASSIGNMENT);
            auditService.logCaseAction(existingCase.getId(), "CASE_APPROVED", approvedBy, 
                "Case approved. Comments: " + (comments != null ? comments : "None"));
            startWorkflow(existingCase, approvedBy);
        } else {
            existingCase.setStatus(CaseStatus.DRAFT);
            auditService.logCaseAction(existingCase.getId(), "CASE_REJECTED", approvedBy, 
                "Case rejected. Comments: " + (comments != null ? comments : "None"));
            createRevisionTask(existingCase, comments, approvedBy);
        }
        
        existingCase.setUpdatedBy(approvedBy);
        existingCase.setUpdatedAt(LocalDateTime.now());
        
        return caseRepository.save(existingCase);
    }
    
    private CaseModel handleStatusUpdate(CaseModel existingCase, CaseStatus newStatus, String updatedBy) {
        CaseStatus oldStatus = existingCase.getStatus();
        existingCase.setStatus(newStatus);
        existingCase.setUpdatedAt(LocalDateTime.now());
        
        CaseModel updatedCase = caseRepository.save(existingCase);
        auditService.logCaseStatusChange(existingCase.getId(), updatedBy, 
            oldStatus.toString(), newStatus.toString());
        
        return updatedCase;
    }
    
    private void updateCaseFields(CaseModel existingCase, CaseRequestDto updateRequest) {
        if (updateRequest.getCaseType() != null) existingCase.setCaseType(updateRequest.getCaseType());
        if (updateRequest.getPriority() != null) existingCase.setPriority(updateRequest.getPriority());
        if (updateRequest.getDescription() != null) existingCase.setDescription(updateRequest.getDescription());
        if (updateRequest.getEntity() != null) existingCase.setEntity(updateRequest.getEntity());
        if (updateRequest.getAlertId() != null) existingCase.setAlertId(updateRequest.getAlertId());
        if (updateRequest.getRiskScore() != null) existingCase.setRiskScore(updateRequest.getRiskScore());
        if (updateRequest.getTypology() != null) existingCase.setTypology(updateRequest.getTypology());
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
    
    private boolean isCompleteCase(CaseRequestDto caseRequest) {
        return caseRequest.getCaseType() != null &&
               caseRequest.getPriority() != null &&
               caseRequest.getEntity() != null &&
               caseRequest.getAlertId() != null &&
               caseRequest.getTypology() != null;
    }
    
    private boolean requiresApproval(CaseModel caseModel) {
        return "HIGH".equals(caseModel.getPriority()) || 
               "CRITICAL".equals(caseModel.getPriority()) ||
               (caseModel.getRiskScore() != null && caseModel.getRiskScore() > 80);
    }
    
    private String generateCaseNumber() {
        int year = LocalDateTime.now().getYear();
        long count = caseRepository.count() + 1;
        return String.format("CASE-%d-%04d", year, count);
    }
    
    private void createInitialTask(CaseModel savedCase, String createdBy) {
        if (savedCase.getStatus() == CaseStatus.DRAFT) {
            taskService.createTask("Complete Case Creation", 
                "Complete the case creation with all required details",
                savedCase.getId(), createdBy, "NORMAL");
        } else if (savedCase.getStatus() == CaseStatus.READY_FOR_ASSIGNMENT) {
            startWorkflow(savedCase, createdBy);
        }
    }
    
    private void createApprovalTask(CaseModel savedCase, String createdBy) {
        taskService.createTask("Approve Case Creation", 
            "Review and approve case creation for case: " + savedCase.getCaseNumber(),
            savedCase.getId(), "admin", "HIGH");
        auditService.logCaseAction(savedCase.getId(), "APPROVAL_TASK_CREATED", createdBy, 
            "Approval task created");
    }
    
    private void createRevisionTask(CaseModel savedCase, String comments, String approvedBy) {
        taskService.createTask("Complete Case Creation", 
            "Case was rejected. Please revise and resubmit. Reason: " + 
            (comments != null ? comments : "No reason provided"),
            savedCase.getId(), savedCase.getCreatedBy(), "HIGH");
    }
    
    private void startWorkflow(CaseModel savedCase, String performedBy) {
        try {
            String processInstanceId = caseWorkflowService.startCaseWorkflow(savedCase);
            savedCase.setProcessInstanceId(processInstanceId);
            caseRepository.save(savedCase);
            auditService.logCaseAction(savedCase.getId(), "WORKFLOW_STARTED", performedBy, 
                "Investigation workflow started");
        } catch (Exception e) {
            logger.error("Failed to start workflow for case: {}", savedCase.getId(), e);
        }
    }
}
