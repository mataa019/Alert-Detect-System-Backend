package com.example.alert_detect_system.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "cases")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class CaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String caseNumber;
    
    private String caseType;
    
    private String priority;
    
    private String entity;
    
    private String alertId;
    
    private CaseStatus status;
    
    private String description;
    
    private Double riskScore;
    
    private String typology;
    
    private String createdBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
      private String processInstanceId;
    
    // Manual setter for caseNumber (in case Lombok isn't working)
    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }
    
    public String getCaseNumber() {
        return caseNumber;
    }
}
