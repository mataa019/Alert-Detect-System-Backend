package com.example.alert_detect_system.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class TaskModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID caseId;
    
    @Column(nullable = false)
    private String taskName;
    
    private String assignee;
    
    private String candidateGroup;
    
    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime dueDate;
    
    private String processInstanceId;
    
    private String taskDefinitionKey;
    
    private String description;
    
    // Constructors
    public TaskModel() {
        this.createdAt = LocalDateTime.now();
        this.status = "CREATED";
    }
    
    public TaskModel(UUID caseId, String taskName, String candidateGroup) {
        this();
        this.caseId = caseId;
        this.taskName = taskName;
        this.candidateGroup = candidateGroup;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    
    public String getCandidateGroup() { return candidateGroup; }
    public void setCandidateGroup(String candidateGroup) { this.candidateGroup = candidateGroup; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    
    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String taskDefinitionKey) { this.taskDefinitionKey = taskDefinitionKey; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}