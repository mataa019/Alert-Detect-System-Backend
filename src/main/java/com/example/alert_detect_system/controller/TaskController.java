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
    
    // Get my tasks
    @GetMapping("/my/{assignee}")
    public ResponseEntity<List<Task>> getMyTasks(@PathVariable String assignee) {
        return ResponseEntity.ok(taskService.getMyTasks(assignee));
    }
    
    // Get group tasks
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Task>> getGroupTasks(@PathVariable String groupId) {
        return ResponseEntity.ok(taskService.getGroupTasks(groupId));
    }
    
    // Get specific task
    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(@PathVariable String taskId) {
        Task task = taskService.getTaskById(taskId);
        return task != null ? ResponseEntity.ok(task) : ResponseEntity.notFound().build();
    }
      // Get tasks by case
    @GetMapping("/by-case/{caseId}")
    public ResponseEntity<List<TaskModel>> getTasksByCaseId(@PathVariable UUID caseId) {
        return ResponseEntity.ok(taskService.getTasksByCaseId(caseId));
    }
      // Assign task (JSON only)
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
      // Complete task (JSON only)
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
            return ResponseEntity.ok("Task completed successfully");        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Update task (as per API spec: PUT /api/task/update/:task-id)
    @PutMapping("/update/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable String taskId, @RequestBody Map<String, Object> updateData) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("TaskId is required");
            }
            
            // Get task details first
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if this is a "Complete Case Creation" task
            String taskName = task.getName();
            if (!"Complete Case Creation".equals(taskName)) {
                return ResponseEntity.badRequest().body("Only 'Complete Case Creation' tasks can be updated");
            }
            
            // Complete the current task and create new "Approve Case Creation" task
            Map<String, Object> variables = new HashMap<>();
            variables.put("action", "CASE_UPDATED");
            variables.put("updatedBy", updateData.getOrDefault("updatedBy", "user"));
            
            // Complete current task
            taskService.completeTask(taskId, variables);
            
            // The BPMN workflow should automatically create the "Approve Case Creation" task
            // Return success response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Task updated successfully");
            response.put("status", "COMPLETED");
            response.put("nextTask", "Approve Case Creation");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating task: " + e.getMessage());
        }
    }
      // Create task record
    @PostMapping("/create")
    public ResponseEntity<?> createTask(@RequestBody TaskModel task) {
        try {
            if (task.getCaseId() == null || task.getTaskName() == null) {
                return ResponseEntity.badRequest().body("CaseId and taskName are required");
            }
            TaskModel savedTask = taskService.saveTask(task);
            return ResponseEntity.ok(savedTask);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * User Story 1: Create a task for a case
     * POST /api/tasks/create/{caseId}
     */
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
    
    /**
     * Get draft cases with "Complete Case Creation" tasks
     * GET /api/tasks/draft-completion
     */
    @GetMapping("/draft-completion")
    public ResponseEntity<List<TaskModel>> getDraftCompletionTasks() {
        List<TaskModel> tasks = taskService.getTasksByType("Complete Case Creation");
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * Get approval tasks for supervisors
     * GET /api/tasks/approval
     */
    @GetMapping("/approval")
    public ResponseEntity<List<TaskModel>> getApprovalTasks() {
        List<TaskModel> approvalTasks = taskService.getTasksByType("Approve Case Creation");
        return ResponseEntity.ok(approvalTasks);
    }
    
    /**
     * Complete approval task
     * PUT /api/tasks/{taskId}/approve
     */
    @PutMapping("/{taskId}/approve")
    public ResponseEntity<Map<String, Object>> approveTask(
            @PathVariable UUID taskId,
            @RequestBody ApprovalTaskRequest request) {
        try {
            // This would complete the task and trigger case approval
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Task approval processed");
            response.put("approved", request.isApproved());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * SUPERVISOR APPROVAL WORKFLOW
     * PUT /api/tasks/{taskId}/approve-case
     * This endpoint handles the complete supervisor approval workflow
     */
    @PutMapping("/{taskId}/approve-case")
    public ResponseEntity<Map<String, Object>> approveCaseCreation(
            @PathVariable String taskId,
            @RequestBody ApprovalRequest request) {
        
        try {
            // Simple check - admin is the supervisor
            String approvedBy = request.getApprovedBy();
            if (approvedBy == null || approvedBy.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Approved by is required"));
            }
            
            // Get the task to validate it exists
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get case ID - simple approach
            UUID caseId = UUID.fromString(task.getProcessVariables().get("caseId").toString());
            Map<String, Object> response = new HashMap<>();
            
            if (request.isApproved()) {
                // APPROVAL FLOW - Simple and straightforward
                // 1. Complete the approval task
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", true);
                variables.put("comments", request.getComments());
                taskService.completeTask(taskId, variables);
                
                // 2. Update case status to READY_FOR_ASSIGNMENT
                caseService.updateCaseStatus(caseId, CaseStatus.READY_FOR_ASSIGNMENT, approvedBy);
                
                // 3. Create "Investigate Case" task for investigations group
                taskService.createInvestigateTask(caseId, "investigations");
                
                response.put("message", "Case approved successfully and assigned to investigations team");
                response.put("status", "APPROVED");
                
            } else {
                // REJECTION FLOW - Simple and straightforward  
                // 1. Complete the approval task
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", false);
                variables.put("comments", request.getComments());
                taskService.completeTask(taskId, variables);
                
                // 2. Update case status back to DRAFT
                caseService.updateCaseStatus(caseId, CaseStatus.DRAFT, approvedBy);
                
                // 3. Create "Complete Case Creation" task for original creator
                String originalCreator = task.getProcessVariables().get("originalCreator").toString();
                taskService.createCompleteTaskForUser(caseId, originalCreator);
                
                response.put("message", "Case rejected and returned to creator for completion");
                response.put("status", "REJECTED");
            }
            
            // Add audit info
            response.put("approvedBy", approvedBy);
            response.put("comments", request.getComments());
            response.put("caseId", caseId.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error processing approval: " + e.getMessage()));
        }
    }

    /**
     * Get pending approval tasks (for supervisors/admin)
     * GET /api/tasks/pending-approvals
     */
    @GetMapping("/pending-approvals")
    public ResponseEntity<List<TaskModel>> getPendingApprovalTasks() {
        try {
            List<TaskModel> pendingTasks = taskService.getPendingApprovalTasks();
            return ResponseEntity.ok(pendingTasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // DTO for task approval
    public static class ApprovalTaskRequest {
        private boolean approved;
        private String comments;
        
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
    
    // DTO for task creation
    public static class CreateTaskRequest {
        private String title;
        private String description;
        private String assignee;
        private String priority;
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }
    
    // DTO for approval requests
    public static class ApprovalRequest {
        private boolean approved;
        private String comments;
        private String approvedBy;
        
        // Getters and setters
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public String getApprovedBy() { return approvedBy; }
        public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    }
}