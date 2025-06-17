package com.example.alert_detect_system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

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
    
    // Constructors
    public CaseRequestDto() {}
    
    // Getters and Setters
    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    
    public String getTypology() { return typology; }
    public void setTypology(String typology) { this.typology = typology; }
}