package com.example.alert_detect_system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    
    public static class StatusRequest {
        public CaseStatus status;
        public String comment;
    }
}