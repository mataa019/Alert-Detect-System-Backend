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
import org.springframework.web.bind.annotation.RequestParam;
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
    @PostMapping("/create")
    public ResponseEntity<?> createCase(@RequestBody CaseRequestDto caseRequest) {
        try {
            CaseModel createdCase = caseService.createCase(caseRequest, "api-user");
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCase);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Get all cases
    @GetMapping("/all")
    public ResponseEntity<List<CaseModel>> getAllCases() {
        List<CaseModel> cases = caseService.getAllCases();
        return ResponseEntity.ok(cases);
    }
    
    // Get case by ID
    @GetMapping("/{caseId}")
    public ResponseEntity<?> getCaseById(@PathVariable UUID caseId) {
        try {
            return caseService.getCaseById(caseId)
                .map(caseEntity -> ResponseEntity.ok(caseEntity))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Update case status
    @PutMapping("/{caseId}/status")
    public ResponseEntity<?> updateCaseStatus(@PathVariable UUID caseId, @RequestParam CaseStatus status) {
        try {
            CaseModel updatedCase = caseService.updateCaseStatus(caseId, status, "api-user");
            return ResponseEntity.ok(updatedCase);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Get cases by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CaseModel>> getCasesByStatus(@PathVariable CaseStatus status) {
        List<CaseModel> cases = caseService.getCasesByStatus(status);
        return ResponseEntity.ok(cases);
    }
}