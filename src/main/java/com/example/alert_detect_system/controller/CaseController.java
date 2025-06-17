package com.example.alert_detect_system.controller;

import com.example.alert_detect_system.dto.CaseRequestDto;

import com.example.alert_detect_system.service.CaseService;

import org.apache.el.stream.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.alert_detect_system.Model.CaseModel;
import com.example.alert_detect_system.Model.CaseStatus;

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
            Optional<CaseModel> caseOpt = caseService.getCaseById(caseId);
            
            if (caseOpt.isPresent()) {
                return ResponseEntity.ok(caseOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
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