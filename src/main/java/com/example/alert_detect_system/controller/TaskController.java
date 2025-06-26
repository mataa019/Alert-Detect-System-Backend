package com.example.alert_detect_system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alert_detect_system.Model.CaseStatus;
import com.example.alert_detect_system.Model.TaskModel;
import com.example.alert_detect_system.dto.TaskAssignDto;
import com.example.alert_detect_system.service.AuditService;
import com.example.alert_detect_system.service.CaseService;
import com.example.alert_detect_system.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private CaseService caseService;
    
    @Autowired
    private AuditService auditService;
    
    /** Get all tasks assigned to a specific user */
    @GetMapping("/my/{assignee}")
    public ResponseEntity<List<Task>> getMyTasks(@PathVariable String assignee) {
        return ResponseEntity.ok(taskService.getMyTasks(assignee));
    }
    
    /** Get all tasks assigned to a specific group */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Task>> getGroupTasks(@PathVariable String groupId) {
        return ResponseEntity.ok(taskService.getGroupTasks(groupId));
    }
    
    /** Get a specific task by its ID */
    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(@PathVariable String taskId) {
        Task task = taskService.getTaskById(taskId);
        return task != null ? ResponseEntity.ok(task) : ResponseEntity.notFound().build();
    }
    
    /** Get all tasks related to a specific case */
    @GetMapping("/by-case/{caseId}")
    public ResponseEntity<List<TaskModel>> getTasksByCaseId(@PathVariable UUID caseId) {
        return ResponseEntity.ok(taskService.getTasksByCaseId(caseId));
    }
    
    /** Assign a task to a user */
    @PostMapping("/assign")
    public ResponseEntity<String> assignTask(@RequestBody TaskAssignDto assignDto) {
        try {
            if (assignDto.getTaskId() == null || assignDto.getAssignee() == null) {
                return ResponseEntity.badRequest().body("TaskId and assignee are required");
            }
            taskService.assignTask(assignDto.getTaskId(), assignDto.getAssignee());
            return ResponseEntity.ok("Task assigned successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /** Complete a task (Flowable or DB) */
    @PostMapping("/complete")
    public ResponseEntity<String> completeTask(@RequestBody Map<String, Object> request) {
        try {
            String taskId = (String) request.get("taskId");
            if (taskId == null || taskId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("TaskId is required");
            }
            Object variablesObj = request.get("variables");
            Map<String, Object> variables = new HashMap<>();
            if (variablesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tempMap = (Map<String, Object>) variablesObj;
                variables = tempMap;
            }
            taskService.completeTask(taskId, variables);
            return ResponseEntity.ok("Task completed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /** Update a task (for case creation workflow) */
    @PutMapping("/update/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable String taskId, @RequestBody Map<String, Object> updateData) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("TaskId is required");
            }
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            String taskName = task.getName();
            if (!"Complete Case Creation".equals(taskName)) {
                return ResponseEntity.badRequest().body("Only 'Complete Case Creation' tasks can be updated");
            }
            Map<String, Object> variables = new HashMap<>();
            variables.put("action", "CASE_UPDATED");
            variables.put("updatedBy", updateData.getOrDefault("updatedBy", "user"));
            taskService.completeTask(taskId, variables);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Task updated successfully");
            response.put("status", "COMPLETED");
            response.put("nextTask", "Approve Case Creation");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating task: " + e.getMessage());
        }
    }
    
    /** Create a new task for a case */
    @PostMapping("/create/{caseId}")
    public ResponseEntity<TaskModel> createTaskForCase(
            @PathVariable UUID caseId,
            @RequestBody CreateTaskRequest request) {
        try {
            TaskModel createdTask = taskService.createTask(
                request.getTitle(),
                request.getDescription(),
                caseId,
                request.getAssignee(),
                request.getPriority()
            );
            return ResponseEntity.ok(createdTask);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /** Supervisor approval workflow for case creation */
    @PutMapping("/{taskId}/approve-case")
    public ResponseEntity<Map<String, Object>> approveCaseCreation(
            @PathVariable String taskId,
            @RequestBody ApprovalRequest request) {
        try {
            String approvedBy = request.getApprovedBy();
            if (approvedBy == null || approvedBy.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Approved by is required"));
            }
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            UUID caseId = UUID.fromString(task.getProcessVariables().get("caseId").toString());
            Map<String, Object> response = new HashMap<>();
            String originalCreator = null;
            if (task.getProcessVariables().containsKey("originalCreator")) {
                originalCreator = task.getProcessVariables().get("originalCreator").toString();
            }
            if (request.isApproved()) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", true);
                variables.put("comments", request.getComments());
                taskService.completeTask(taskId, variables);
                caseService.updateCaseStatus(caseId, CaseStatus.READY_FOR_ASSIGNMENT, approvedBy);
                taskService.createInvestigateTask(caseId, "investigations");
                if (originalCreator != null) {
                    auditService.logCaseAction(caseId, "CASE_APPROVED", approvedBy, "Case approved and assigned to investigations. Notified analyst: " + originalCreator);
                } else {
                    auditService.logCaseAction(caseId, "CASE_APPROVED", approvedBy, "Case approved and assigned to investigations.");
                }
                response.put("message", "Case approved successfully and assigned to investigations team");
                response.put("status", "APPROVED");
            } else {
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", false);
                variables.put("comments", request.getComments());
                taskService.completeTask(taskId, variables);
                caseService.updateCaseStatus(caseId, CaseStatus.DRAFT, approvedBy);
                if (originalCreator != null) {
                    taskService.createCompleteTaskForUser(caseId, originalCreator);
                    auditService.logCaseAction(caseId, "CASE_REJECTED", approvedBy, "Case rejected and returned to analyst: " + originalCreator);
                } else {
                    auditService.logCaseAction(caseId, "CASE_REJECTED", approvedBy, "Case rejected and returned to analyst.");
                }
                response.put("message", "Case rejected and returned to creator for completion");
                response.put("status", "REJECTED");
            }
            response.put("approvedBy", approvedBy);
            response.put("comments", request.getComments());
            response.put("caseId", caseId.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error processing approval: " + e.getMessage()));
        }
    }
    
    /** Get DB tasks by assignee */
    @GetMapping("/by-assignee/{assignee}")
    public ResponseEntity<List<TaskModel>> getTasksByAssignee(@PathVariable String assignee) {
        return ResponseEntity.ok(taskService.getTasksByAssignee(assignee));
    }
    
    public static class CreateTaskRequest {
        private String title;
        private String description;
        private String assignee;
        private String priority;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }
    
    public static class ApprovalRequest {
        private boolean approved;
        private String comments;
        private String approvedBy;
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public String getApprovedBy() { return approvedBy; }
        public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    }
}