package com.example.alert_detect_system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.alert_detect_system.Model.CaseModel;
import com.example.alert_detect_system.Model.CaseStatus;
import com.example.alert_detect_system.dto.CaseRequestDto;
import com.example.alert_detect_system.service.AuditService;
import com.example.alert_detect_system.service.CaseService;
import com.example.alert_detect_system.service.TaskService;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class CaseController {
    private static final Logger logger = LoggerFactory.getLogger(CaseController.class);
    
    @Autowired
    private CaseService caseService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private AuditService auditService;
    
    /**
     * 1. CREATE CASE - Single endpoint for all case creation
     * POST /api/cases
     */
    @PostMapping
    public ResponseEntity<CaseModel> createCase(@RequestBody CaseRequestDto caseRequest) {
        String createdBy = caseRequest.getCreatedBy() != null ? caseRequest.getCreatedBy() : "user";
        // Always assign to admin1
        caseRequest.setAssignee("admin1");
        CaseModel createdCase = caseService.createCase(caseRequest, createdBy);
        return ResponseEntity.ok(createdCase);
    }
    
    /**
     * 2. GET CASES - Single endpoint with optional filtering
     * GET /api/cases?status=DRAFT&creator=analyst&pendingApproval=true
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String creator,
            @RequestParam(required = false, defaultValue = "false") boolean pendingApproval) {
        List<CaseModel> cases;
        if (pendingApproval) {
            cases = caseService.getCasesByStatus(CaseStatus.PENDING_CASE_CREATION_APPROVAL);
        } else if (status != null) {
            try {
                CaseStatus caseStatus = CaseStatus.valueOf(status.toUpperCase());
                cases = caseService.getCasesByStatus(caseStatus);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (creator != null) {
            cases = caseService.getCasesByCreatedBy(creator);
        } else {
            cases = caseService.getAllCases();
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (CaseModel c : cases) {
            Map<String, Object> map = new HashMap<>();
            map.put("case", c);
            // Find the active approval task for this case
            var approvalTask = taskService.getPendingApprovalTasks().stream()
                .filter(t -> t.getCaseId().equals(c.getId()))
                .findFirst().orElse(null);
            if (approvalTask != null) {
                map.put("taskId", approvalTask.getId().toString());
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 3. GET CASE BY ID
     * GET /api/cases/{caseId}
     */
    @GetMapping("/{caseId}")
    public ResponseEntity<CaseModel> getCaseById(@PathVariable UUID caseId) {
        return caseService.getCaseById(caseId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 4. UPDATE CASE - Single endpoint for all case updates
     * PUT /api/cases/{caseId}?action=complete|approve|update|status
     */
    @PutMapping("/{caseId}")
    public ResponseEntity<Object> updateCase(
            @PathVariable UUID caseId,
            @RequestParam String action,
            @RequestBody Map<String, Object> requestBody) {
        
        try {
            Optional<CaseModel> existingCase = caseService.getCaseById(caseId);
            if (existingCase.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            String updatedBy = (String) requestBody.getOrDefault("updatedBy", "user");
            
            switch (action.toLowerCase()) {
                case "complete" -> {
                    logger.debug("[API] Received COMPLETE action for caseId: {} by user: {}. Request body: {}", caseId, updatedBy, requestBody);
                    // Complete draft case using consolidated method
                    CaseRequestDto updateRequest = mapToDto(requestBody);
                    CaseModel completedCase = caseService.performCaseAction(caseId, "complete", updateRequest, updatedBy, Map.of());
                    logger.debug("[API] Case after COMPLETE action: {}", completedCase);
                    return ResponseEntity.ok(completedCase);
                }
                case "approve" -> {
                    // Approve/reject case using consolidated method
                    Object approvedObj = requestBody.getOrDefault("approved", false);
                    boolean approved;
                    if (approvedObj instanceof Boolean approvedBool) {
                        approved = approvedBool;
                    } else if (approvedObj instanceof String approvedStr) {
                        approved = Boolean.parseBoolean(approvedStr);
                    } else {
                        approved = false;
                    }
                    String comments = (String) requestBody.getOrDefault("comments", "");
                    Map<String, Object> approvalParams = Map.of("approved", approved, "comments", comments);
                    CaseModel caseToApprove = existingCase.get();
                    // Debug log: show current status and input
                    System.out.println("[DEBUG] Approve action: caseId=" + caseId + ", currentStatus=" + caseToApprove.getStatus() + ", approved=" + approved + ", updatedBy=" + updatedBy);
                    // Only allow approval/rejection if case is in PENDING_CASE_CREATION_APPROVAL
                    if (caseToApprove.getStatus() != CaseStatus.PENDING_CASE_CREATION_APPROVAL) {
                        auditService.logCaseAction(caseId, "UNAUTHORIZED_APPROVAL_ATTEMPT", updatedBy, "Tried to approve/reject case not in PENDING_CASE_CREATION_APPROVAL");
                        System.out.println("[DEBUG] Approval blocked: status is not PENDING_CASE_CREATION_APPROVAL");
                        return ResponseEntity.status(403).body("Case is not pending approval");
                    }
                    // Admin-only approval check
                    if (!"admin1".equalsIgnoreCase(updatedBy)) {
                        auditService.logCaseAction(caseId, "UNAUTHORIZED_APPROVAL_ATTEMPT", updatedBy, "Non-admin tried to approve/reject case");
                        System.out.println("[DEBUG] Approval blocked: user is not admin1");
                        return ResponseEntity.status(403).body("Only admin can approve or reject cases");
                    }
                    CaseModel approvedCase = caseService.performCaseAction(caseId, "approve", null, updatedBy, approvalParams);
                    // Debug log: show new status
                    System.out.println("[DEBUG] Approval processed: newStatus=" + approvedCase.getStatus());
                    // Log approval/rejection event
                    auditService.logCaseAction(caseId, approved ? "CASE_APPROVED" : "CASE_REJECTED", updatedBy, comments);
                    Map<String, Object> response = new HashMap<>();
                    response.put("case", approvedCase);
                    response.put("message", approved ? "Case approved and workflow started" : "Case rejected");
                    return ResponseEntity.ok(response);
                }
                case "status" -> {
                    // Update status using consolidated method
                    String statusStr = (String) requestBody.get("status");
                    CaseStatus newStatus = CaseStatus.valueOf(statusStr.toUpperCase());
                    Map<String, Object> statusParams = Map.of("status", newStatus);
                    CaseModel updatedCase = caseService.performCaseAction(caseId, "status", null, updatedBy, statusParams);
                    return ResponseEntity.ok(updatedCase);
                }
                case "update" -> {
                    // Update case details using consolidated method
                    // Allow updates for DRAFT cases or if user is the creator/admin
                    CaseRequestDto caseUpdate = mapToDto(requestBody);
                    CaseModel updated = caseService.performCaseAction(caseId, "update", caseUpdate, updatedBy, Map.of());
                    return ResponseEntity.ok(updated);
                }
                case "edit" -> {
                    // Same as update - general case editing
                    CaseRequestDto editRequest = mapToDto(requestBody);
                    CaseModel editedCase = caseService.performCaseAction(caseId, "update", editRequest, updatedBy, Map.of());
                    return ResponseEntity.ok(editedCase);
                }
                default -> {
                    return ResponseEntity.badRequest().body("Invalid action: " + action);
                }
            }
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 5. GET RECENT CASES - For dashboard display
     * GET /api/cases/recent?limit=10&user=analyst
     */
    @GetMapping("/recent")
    public ResponseEntity<List<CaseModel>> getRecentCases(
            @RequestParam(required = false, defaultValue = "10") int limit,
            @RequestParam(required = false) String user) {
        
        try {
            List<CaseModel> recentCases;
            if (user != null && !user.trim().isEmpty()) {
                // Get recent cases for specific user
                recentCases = caseService.getRecentCasesByUser(user, limit);
            } else {
                // Get recent cases for all users (admin view)
                recentCases = caseService.getRecentCases(limit);
            }
            return ResponseEntity.ok(recentCases);
        } catch (Exception e) {
            // Return empty list on error to match return type
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    /**
     * 6. DELETE CASE
     * DELETE /api/cases/{caseId}?deletedBy=user
     */
    @DeleteMapping("/{caseId}")
    public ResponseEntity<?> deleteCase(
            @PathVariable UUID caseId,
            @RequestParam(required = false, defaultValue = "user") String deletedBy) {
        
        try {
            caseService.deleteCase(caseId, deletedBy);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Case deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error deleting case: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * ABANDON CASE endpoint
     * PUT /api/cases/abandon/{caseId}
     * Body: { "abandonedBy": "username", "reason": "..." }
     */
    @PutMapping("/abandon/{caseId}")
    public ResponseEntity<?> abandonCase(
            @PathVariable UUID caseId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String abandonedBy = (String) requestBody.getOrDefault("abandonedBy", "user");
            String reason = (String) requestBody.getOrDefault("reason", "No reason provided");
            Optional<CaseModel> caseOpt = caseService.getCaseById(caseId);
            if (caseOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            CaseModel caseModel = caseOpt.get();
            if (caseModel.getStatus() != CaseStatus.DRAFT) {
                return ResponseEntity.badRequest().body("Case must be in DRAFT status to abandon.");
            }
            if (!abandonedBy.equals(caseModel.getCreatedBy())) {
                return ResponseEntity.status(403).body("Only the creator can abandon this draft case.");
            }
            // Update status to ABANDONED
            caseService.updateCaseStatus(caseId, CaseStatus.ABANDONED, abandonedBy);
            // Close associated draft task (Complete New Case)
            taskService.closeDraftTaskForCase(caseId);
            // Log audit event
            auditService.logCaseAction(caseId, "CASE_ABANDONED", abandonedBy, "Reason: " + reason);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Case abandoned successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error abandoning case: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // Helper method to convert Map to DTO
    private CaseRequestDto mapToDto(Map<String, Object> requestBody) {
        CaseRequestDto dto = new CaseRequestDto();
        if (requestBody.get("caseType") != null) dto.setCaseType((String) requestBody.get("caseType"));
        if (requestBody.get("priority") != null) dto.setPriority((String) requestBody.get("priority"));
        if (requestBody.get("description") != null) dto.setDescription((String) requestBody.get("description"));
        if (requestBody.get("entity") != null) dto.setEntity((String) requestBody.get("entity"));
        if (requestBody.get("alertId") != null) dto.setAlertId((String) requestBody.get("alertId"));
        if (requestBody.get("typology") != null) dto.setTypology((String) requestBody.get("typology"));
        if (requestBody.get("riskScore") != null) {
            Object riskScore = requestBody.get("riskScore");
            if (riskScore instanceof Number number) {
                dto.setRiskScore(number.doubleValue());
            }
        }
        if (requestBody.get("createdBy") != null) dto.setCreatedBy((String) requestBody.get("createdBy"));
        if (requestBody.get("assignee") != null) dto.setAssignee((String) requestBody.get("assignee"));
        return dto;
    }
}