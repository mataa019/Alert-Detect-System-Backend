package com.example.alert_detect_system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class CaseRequestDto {
    
    private String caseType;
    
    private String priority;
    
    private String entity;
    
    private String alertId;
    
    private String description;
    
    @DecimalMin(value = "0.0", message = "Risk score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Risk score must be between 0 and 100")
    private Double riskScore;
    
    private String typology;
    
    
}