package com.example.alert_detect_system.dto;

public class TaskAssignDto {
    
    private String taskId;
    private String assignee;
    private String comment;
    
    public TaskAssignDto() {}
    
    public TaskAssignDto(String taskId, String assignee, String comment) {
        this.taskId = taskId;
        this.assignee = assignee;
        this.comment = comment;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getAssignee() {
        return assignee;
    }
    
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
}