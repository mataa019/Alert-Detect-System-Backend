package com.example.alert_detect_system.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alert_detect_system.Model.CaseModel;
import com.example.alert_detect_system.dto.CaseRequestDto;
import com.example.alert_detect_system.service.CaseService;

@RestController
@RequestMapping("/cases")
public class CaseController {
    
    @Autowired
    private CaseService caseService;
    
    @PostMapping("/create")
    public ResponseEntity<CaseModel> createCase(@RequestBody CaseRequestDto caseRequest) {
        CaseModel createdCase = caseService.createCase(caseRequest, "user");
        return ResponseEntity.ok(createdCase);
    }
    
    @GetMapping("/all")
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
}