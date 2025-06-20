package com.example.alert_detect_system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.flowable.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    
    @Autowired
    private RepositoryService repositoryService;
    
    @PostMapping("/create")
    public ResponseEntity<CaseModel> createCase(@RequestBody CaseRequestDto caseRequest) {
        CaseModel createdCase = caseService.createCase(caseRequest, "user");
        return ResponseEntity.ok(createdCase);
    }
      @GetMapping
    public ResponseEntity<List<CaseModel>> getAllCases() {
        List<CaseModel> cases = caseService.getAllCases();
        return ResponseEntity.ok(cases);
    }
      @GetMapping("/{caseId}")
    public ResponseEntity<CaseModel> getCaseById(@PathVariable UUID caseId) {
        return caseService.getCaseById(caseId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
      @PutMapping("/{caseId}/status")
    public ResponseEntity<CaseModel> updateCaseStatus(@PathVariable UUID caseId, @RequestBody StatusRequest request) {
        CaseModel updatedCase = caseService.updateCaseStatus(caseId, request.status, "user");
        return ResponseEntity.ok(updatedCase);
    }
    
    // Update case details (for completing DRAFT cases)
    @PutMapping("/{caseId}")
    public ResponseEntity<?> updateCase(@PathVariable UUID caseId, @RequestBody CaseRequestDto caseUpdate) {
        try {
            // Validate that case is in DRAFT status
            Optional<CaseModel> existingCase = caseService.getCaseById(caseId);
            if (existingCase.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            if (!CaseStatus.DRAFT.equals(existingCase.get().getStatus())) {
                return ResponseEntity.badRequest().body("Only DRAFT cases can be updated");
            }
            
            // Update case details
            CaseModel updatedCase = caseService.updateCase(caseId, caseUpdate, "user");
            return ResponseEntity.ok(updatedCase);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating case: " + e.getMessage());
        }
    }
      @GetMapping("/by-status/{status}")
    public ResponseEntity<List<CaseModel>> getCasesByStatus(@PathVariable CaseStatus status) {
        List<CaseModel> cases = caseService.getCasesByStatus(status);
        return ResponseEntity.ok(cases);
    }
    
    // Test endpoint to verify connection
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Backend connection successful!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    // Test endpoint to check if Flowable process is deployed
    @GetMapping("/flowable-test")
    public ResponseEntity<Map<String, Object>> testFlowableProcessDeployment() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long count = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey("caseInvestigationProcess")
                    .count();
            
            result.put("processDeployed", count > 0);
            result.put("processCount", count);
            result.put("message", count > 0 ? "Process is deployed successfully" : "Process not found");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * User Story 2: Complete case creation (Draft -> Ready/Pending Approval)
     * PUT /api/cases/{caseId}/complete
     */
    @PutMapping("/{caseId}/complete")
    public ResponseEntity<CaseModel> completeCaseCreation(
            @PathVariable UUID caseId, 
            @RequestBody CaseRequestDto updateRequest) {
        try {
            CaseModel completedCase = caseService.completeCaseCreation(caseId, updateRequest, "user");
            return ResponseEntity.ok(completedCase);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Approve case creation (Supervisor function)
     * PUT /api/cases/{caseId}/approve
     */
    @PutMapping("/{caseId}/approve")
    public ResponseEntity<Map<String, Object>> approveCaseCreation(
            @PathVariable UUID caseId, 
            @RequestBody ApprovalRequest request) {
        try {
            CaseModel approvedCase = caseService.approveCaseCreation(caseId, request.isApproved(), 
                request.getComments(), "supervisor"); // In real app, get from auth
            
            Map<String, Object> response = new HashMap<>();
            response.put("case", approvedCase);
            response.put("message", request.isApproved() ? 
                "Case approved and workflow started" : "Case rejected");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get pending approval cases
     * GET /api/cases/pending-approval
     */
    @GetMapping("/pending-approval")
    public ResponseEntity<List<CaseModel>> getPendingApprovalCases() {
        List<CaseModel> pendingCases = caseService.getCasesByStatus(CaseStatus.PENDING_CASE_CREATION_APPROVAL);
        return ResponseEntity.ok(pendingCases);
    }
    
    // DTO for approval request
    public static class ApprovalRequest {
        private boolean approved;
        private String comments;
        
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
    
    public static class StatusRequest {
        public CaseStatus status;
        public String comment;
    }
}