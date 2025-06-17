package com.example.alert_detect_system.service;

import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    
    @Autowired
    private org.flowable.engine.TaskService flowableTaskService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Get all tasks assigned to a specific user
     */
    public List<Task> getMyTasks(String assignee) {
        logger.info("Getting tasks for user: {}", assignee);
        
        return flowableTaskService.createTaskQuery()
            .taskAssignee(assignee)
            .orderByTaskCreateTime()
            .desc()
            .list();
    }
    
    /**
     * Get all tasks available for a user's groups (candidate tasks)
     */
    public List<Task> getGroupTasks(String userId) {
        logger.info("Getting group tasks for user: {}", userId);
        
        return flowableTaskService.createTaskQuery()
            .taskCandidateUser(userId)
            .orderByTaskCreateTime()
            .desc()
            .list();
    }
    
    /**
     * Get all tasks for a specific case
     */
    public List<Task> getTasksByCaseId(UUID caseId) {
        logger.info("Getting tasks for case: {}", caseId);
        
        return flowableTaskService.createTaskQuery()
            .processVariableValueEquals("caseId", caseId.toString())
            .orderByTaskCreateTime()
            .desc()
            .list();
    }
    
    /**
     * Claim/Assign a task to a user
     */
    public void assignTask(String taskId, String assignee) {
        logger.info("Assigning task {} to user: {}", taskId, assignee);
        
        try {
            // Get task details before assignment
            Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
            
            if (task == null) {
                throw new RuntimeException("Task not found with ID: " + taskId);
            }
            
            // Assign the task
            flowableTaskService.setAssignee(taskId, assignee);
            
            // Log the assignment
            String caseId = getCaseIdFromTask(taskId);
            if (caseId != null) {
                auditService.logCaseAction(
                    UUID.fromString(caseId),
                    "TASK_ASSIGNED",
                    assignee,
                    String.format("Task '%s' assigned to %s", task.getName(), assignee)
                );
            }
            
            logger.info("Task {} successfully assigned to {}", taskId, assignee);
            
        } catch (Exception e) {
            logger.error("Error assigning task {} to user {}", taskId, assignee, e);
            throw new RuntimeException("Failed to assign task: " + e.getMessage());
        }
    }
    
    /**
     * Complete a task with variables
     */
    public void completeTask(String taskId, Map<String, Object> variables, String completedBy) {
        logger.info("Completing task {} by user: {}", taskId, completedBy);
        
        try {
            // Get task details before completion
            Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
            
            if (task == null) {
                throw new RuntimeException("Task not found with ID: " + taskId);
            }
            
            // Ensure the user can complete this task
            if (task.getAssignee() != null && !task.getAssignee().equals(completedBy)) {
                throw new RuntimeException("Task is assigned to different user");
            }
            
            // Add completion metadata
            if (variables == null) {
                variables = new HashMap<>();
            }
            variables.put("completedBy", completedBy);
            variables.put("completedAt", System.currentTimeMillis());
            
            // Complete the task
            flowableTaskService.complete(taskId, variables);
            
            // Log the completion
            String caseId = getCaseIdFromTask(taskId);
            if (caseId != null) {
                auditService.logCaseAction(
                    UUID.fromString(caseId),
                    "TASK_COMPLETED",
                    completedBy,
                    String.format("Task '%s' completed", task.getName())
                );
            }
            
            logger.info("Task {} successfully completed by {}", taskId, completedBy);
            
        } catch (Exception e) {
            logger.error("Error completing task {} by user {}", taskId, completedBy, e);
            throw new RuntimeException("Failed to complete task: " + e.getMessage());
        }
    }
    
    /**
     * Complete a task with approval/rejection
     */
    public void completeApprovalTask(String taskId, boolean approved, String comment, String completedBy) {
        logger.info("Completing approval task {} with decision: {}", taskId, approved);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        variables.put("approvalComment", comment);
        variables.put("approvedBy", completedBy);
        
        completeTask(taskId, variables, completedBy);
        
        // Update case status based on approval
        String caseId = getCaseIdFromTask(taskId);
        if (caseId != null) {
            // You might want to update case status here
            auditService.logCaseAction(
                UUID.fromString(caseId),
                approved ? "CASE_APPROVED" : "CASE_REJECTED",
                completedBy,
                comment
            );
        }
    }
    
    /**
     * Get task details by ID
     */
    public Task getTaskById(String taskId) {
        return flowableTaskService.createTaskQuery()
            .taskId(taskId)
            .singleResult();
    }
    
    /**
     * Get all active tasks (for admin/monitoring)
     */
    public List<Task> getAllActiveTasks() {
        return flowableTaskService.createTaskQuery()
            .orderByTaskCreateTime()
            .desc()
            .list();
    }
    
    /**
     * Get tasks by candidate group
     */
    public List<Task> getTasksByGroup(String groupId) {
        return flowableTaskService.createTaskQuery()
            .taskCandidateGroup(groupId)
            .orderByTaskCreateTime()
            .desc()
            .list();
    }
    
    /**
     * Delegate task to another user
     */
    public void delegateTask(String taskId, String delegateTo, String delegatedBy) {
        logger.info("Delegating task {} from {} to {}", taskId, delegatedBy, delegateTo);
        
        try {
            flowableTaskService.delegateTask(taskId, delegateTo);
            
            String caseId = getCaseIdFromTask(taskId);
            if (caseId != null) {
                auditService.logCaseAction(
                    UUID.fromString(caseId),
                    "TASK_DELEGATED",
                    delegatedBy,
                    String.format("Task delegated to %s", delegateTo)
                );
            }
            
        } catch (Exception e) {
            logger.error("Error delegating task", e);
            throw new RuntimeException("Failed to delegate task: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to get case ID from task variables
     */
    private String getCaseIdFromTask(String taskId) {
        try {
            Map<String, Object> variables = flowableTaskService.getVariables(taskId);
            return (String) variables.get("caseId");
        } catch (Exception e) {
            logger.warn("Could not retrieve caseId from task {}", taskId);
            return null;
        }
    }
    
    /**
     * Get task statistics for monitoring
     */
    public Map<String, Long> getTaskStatistics() {
        Map<String, Long> stats = new HashMap<>();
        
        TaskQuery baseQuery = flowableTaskService.createTaskQuery();
        
        stats.put("totalActiveTasks", baseQuery.count());
        stats.put("unassignedTasks", baseQuery.taskUnassigned().count());
        stats.put("assignedTasks", baseQuery.taskAssigned().count());
        stats.put("overdueeTasks", baseQuery.taskDueBefore(new java.util.Date()).count());
        
        return stats;
    }
}
