package com.example.alert_detect_system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import com.example.alert_detect_system.service.CaseService;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class CaseController {
    
    @Autowired
    private CaseService caseService;
    
    /**
     * 1. CREATE CASE - Single endpoint for all case creation
     * POST /api/cases
     */
    @PostMapping
    public ResponseEntity<CaseModel> createCase(@RequestBody CaseRequestDto caseRequest) {
        String createdBy = caseRequest.getCreatedBy() != null ? caseRequest.getCreatedBy() : "user";
        CaseModel createdCase = caseService.createCase(caseRequest, createdBy);
        return ResponseEntity.ok(createdCase);
    }
    
    /**
     * 2. GET CASES - Single endpoint with optional filtering
     * GET /api/cases?status=DRAFT&creator=analyst&pendingApproval=true
     */
    @GetMapping
    public ResponseEntity<List<CaseModel>> getCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String creator,
            @RequestParam(required = false, defaultValue = "false") boolean pendingApproval) {
        
        // Filter by pending approval
        if (pendingApproval) {
            List<CaseModel> pendingCases = caseService.getCasesByStatus(CaseStatus.PENDING_CASE_CREATION_APPROVAL);
            return ResponseEntity.ok(pendingCases);
        }
        
        // Filter by status
        if (status != null) {
            try {
                CaseStatus caseStatus = CaseStatus.valueOf(status.toUpperCase());
                List<CaseModel> cases = caseService.getCasesByStatus(caseStatus);
                return ResponseEntity.ok(cases);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        // Filter by creator
        if (creator != null) {
            List<CaseModel> cases = caseService.getCasesByCreatedBy(creator);
            return ResponseEntity.ok(cases);
        }
        
        // Default: return all cases
        List<CaseModel> allCases = caseService.getAllCases();
        return ResponseEntity.ok(allCases);
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
                case "complete":
                    // Complete draft case using consolidated method
                    CaseRequestDto updateRequest = mapToDto(requestBody);
                    CaseModel completedCase = caseService.performCaseAction(caseId, "complete", updateRequest, updatedBy, Map.of());
                    return ResponseEntity.ok(completedCase);
                    
                case "approve":
                    // Approve/reject case using consolidated method
                    Object approvedObj = requestBody.getOrDefault("approved", false);
                    boolean approved;
                    if (approvedObj instanceof Boolean) {
                        approved = (Boolean) approvedObj;
                    } else if (approvedObj instanceof String) {
                        approved = Boolean.parseBoolean((String) approvedObj);
                    } else {
                        approved = false;
                    }
                    String comments = (String) requestBody.getOrDefault("comments", "");
                    Map<String, Object> approvalParams = Map.of("approved", approved, "comments", comments);
                    CaseModel approvedCase = caseService.performCaseAction(caseId, "approve", null, updatedBy, approvalParams);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("case", approvedCase);
                    response.put("message", approved ? "Case approved and workflow started" : "Case rejected");
                    return ResponseEntity.ok(response);
                    
                case "status":
                    // Update status using consolidated method
                    String statusStr = (String) requestBody.get("status");
                    CaseStatus newStatus = CaseStatus.valueOf(statusStr.toUpperCase());
                    Map<String, Object> statusParams = Map.of("status", newStatus);
                    CaseModel updatedCase = caseService.performCaseAction(caseId, "status", null, updatedBy, statusParams);
                    return ResponseEntity.ok(updatedCase);
                    
                case "update":
                    // Update case details using consolidated method
                    // Allow updates for DRAFT cases or if user is the creator/admin
                    CaseRequestDto caseUpdate = mapToDto(requestBody);
                    CaseModel updated = caseService.performCaseAction(caseId, "update", caseUpdate, updatedBy, Map.of());
                    return ResponseEntity.ok(updated);
                
                case "edit":
                    // Same as update - general case editing
                    CaseRequestDto editRequest = mapToDto(requestBody);
                    CaseModel editedCase = caseService.performCaseAction(caseId, "update", editRequest, updatedBy, Map.of());
                    return ResponseEntity.ok(editedCase);
                    
                default:
                    return ResponseEntity.badRequest().body("Invalid action: " + action);
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
            return ResponseEntity.badRequest().build();
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
     * 7. GET USER ROLE (mocked for demo)
     * GET /api/user/role?user=username
     */
    @GetMapping("/user/role")
    public ResponseEntity<Map<String, String>> getUserRole(@RequestParam String user) {
        // Mock logic: 'admin' user is Admin, others are Analyst
        String role = "analyst";
        if ("admin".equalsIgnoreCase(user)) {
            role = "admin";
        }
        Map<String, String> response = new HashMap<>();
        response.put("role", role);
        return ResponseEntity.ok(response);
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
            if (riskScore instanceof Number) {
                dto.setRiskScore(((Number) riskScore).doubleValue());
            }
        }
        if (requestBody.get("createdBy") != null) dto.setCreatedBy((String) requestBody.get("createdBy"));
        if (requestBody.get("assignee") != null) dto.setAssignee((String) requestBody.get("assignee"));
        return dto;
    }
}