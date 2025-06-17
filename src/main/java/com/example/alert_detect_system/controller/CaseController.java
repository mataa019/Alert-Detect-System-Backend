package com.example.alert_detect_system.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class CaseController {
    
    @Autowired
    private CaseService caseService;
    
    // Create case
    @PostMapping
    public ResponseEntity<CaseModel> createCase(@RequestBody CaseRequestDto caseRequest) {
        try {
            if (caseRequest.getCaseType() == null || caseRequest.getPriority() == null || 
                caseRequest.getEntity() == null || caseRequest.getAlertId() == null) {
                return ResponseEntity.badRequest().body(null);
            }
            CaseModel createdCase = caseService.createCase(caseRequest, "api-user");
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCase);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // Get all cases
    @GetMapping
    public ResponseEntity<List<CaseModel>> getAllCases() {
        try {
            List<CaseModel> cases = caseService.getAllCases();
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
      // Get case by ID
    @GetMapping("/{caseId}")
    public ResponseEntity<CaseModel> getCaseById(@PathVariable UUID caseId) {
        try {
            if (caseId == null) {
                return ResponseEntity.badRequest().body(null);
            }
            return caseService.getCaseById(caseId)
                .map(caseEntity -> ResponseEntity.ok(caseEntity))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // Update case status
    @PutMapping("/{caseId}/status")
    public ResponseEntity<CaseModel> updateCaseStatus(@PathVariable UUID caseId, @RequestBody StatusUpdateRequest request) {
        try {
            if (caseId == null || request.getStatus() == null) {
                return ResponseEntity.badRequest().body(null);
            }
            CaseModel updatedCase = caseService.updateCaseStatus(caseId, request.getStatus(), "api-user");
            return ResponseEntity.ok(updatedCase);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // Get cases by status
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<CaseModel>> getCasesByStatus(@PathVariable CaseStatus status) {
        try {
            if (status == null) {
                return ResponseEntity.badRequest().body(null);
            }
            List<CaseModel> cases = caseService.getCasesByStatus(status);
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // Inner class for status update request
    public static class StatusUpdateRequest {
        private CaseStatus status;
        private String comment;
        
        public CaseStatus getStatus() { return status; }
        public void setStatus(CaseStatus status) { this.status = status; }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}