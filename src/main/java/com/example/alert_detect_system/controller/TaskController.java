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
    
    /**
     * Get all tasks assigned to a specific user (assignee).
     * GET /api/tasks/my/{assignee}
     */
    @GetMapping("/my/{assignee}")
    public ResponseEntity<List<Task>> getMyTasks(@PathVariable String assignee) {
        return ResponseEntity.ok(taskService.getMyTasks(assignee));
    }
    
    /**
     * Get all tasks assigned to a specific group.
     * GET /api/tasks/group/{groupId}
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Task>> getGroupTasks(@PathVariable String groupId) {
        return ResponseEntity.ok(taskService.getGroupTasks(groupId));
    }
    
    /**
     * Get a specific task by its ID.
     * GET /api/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(@PathVariable String taskId) {
        Task task = taskService.getTaskById(taskId);
        return task != null ? ResponseEntity.ok(task) : ResponseEntity.notFound().build();
    }
    
    /**
     * Get all tasks for a specific case by case ID.
     * GET /api/tasks/by-case/{caseId}
     */
    @GetMapping("/by-case/{caseId}")
    public ResponseEntity<List<TaskModel>> getTasksByCaseId(@PathVariable UUID caseId) {
        return ResponseEntity.ok(taskService.getTasksByCaseId(caseId));
    }
    
    /**
     * Assign a task to a user (JSON only).
     * POST /api/tasks/assign
     */
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
    
    /**
     * Complete a task (JSON only).
     * POST /api/tasks/complete
     */
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
    
    /**
     * Update a task (only for 'Complete Case Creation' tasks).
     * PUT /api/tasks/update/{taskId}
     */
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
    
    /**
     * Create a new task record for a case.
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
     * Supervisor approval workflow for case creation.
     * PUT /api/tasks/{taskId}/approve-case
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
            String originalCreator = null;
            if (task.getProcessVariables().containsKey("originalCreator")) {
                originalCreator = task.getProcessVariables().get("originalCreator").toString();
            }
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
                // 4. Log the approval action for the analyst
                if (originalCreator != null) {
                    auditService.logCaseAction(caseId, "CASE_APPROVED", approvedBy, "Case approved and assigned to investigations. Notified analyst: " + originalCreator);
                } else {
                    auditService.logCaseAction(caseId, "CASE_APPROVED", approvedBy, "Case approved and assigned to investigations.");
                }
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
                if (originalCreator != null) {
                    taskService.createCompleteTaskForUser(caseId, originalCreator);
                    auditService.logCaseAction(caseId, "CASE_REJECTED", approvedBy, "Case rejected and returned to analyst: " + originalCreator);
                } else {
                    auditService.logCaseAction(caseId, "CASE_REJECTED", approvedBy, "Case rejected and returned to analyst.");
                }
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
     * Get all DB tasks by assignee (from TaskModel).
     * GET /api/tasks/by-assignee/{assignee}
     */
    @GetMapping("/by-assignee/{assignee}")
    public ResponseEntity<List<TaskModel>> getTasksByAssignee(@PathVariable String assignee) {
        return ResponseEntity.ok(taskService.getTasksByAssignee(assignee));
    }

    /**
     * Assign or reassign a task to a user or work queue (supervisor action).
     * PUT /api/tasks/assign/{taskId}
     *
     * Acceptance Criteria:
     * - Only non-completed tasks can be reassigned.
     * - Tasks can be claimed, unassigned, or reassigned.
     * - Assignments must update task status to either “ASSIGNED” or “UNASSIGNED”.
     * - Reassignment must notify both the old and new owners (if applicable).
     * - Invalid target users must be rejected with a proper error message.
     * - Logs (event, audit, system) must record all actions.
     */
    @PutMapping("/assign/{taskId}")
    public ResponseEntity<?> assignOrReassignTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            // Validate input
            String newAssignee = (String) requestBody.get("assignee");
            String performedBy = (String) requestBody.getOrDefault("performedBy", "supervisor");
            // Get the task
            TaskModel task = taskService.getTaskModelById(taskId);
            if (task == null) {
                return ResponseEntity.badRequest().body("Task not found");
            }
            // Only allow non-completed tasks
            if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                return ResponseEntity.badRequest().body("Cannot reassign a completed task");
            }
            String oldAssignee = task.getAssignee();
            // Unassignment: if assignee is null/empty, remove ownership and set status to UNASSIGNED
            if (newAssignee == null || newAssignee.trim().isEmpty()) {
                // Remove assignee
                taskService.assignTask(taskId, null);
                taskService.updateTaskStatus(taskId, "UNASSIGNED");
                auditService.logTaskAction(taskId, "TASK_UNASSIGNED", performedBy,
                    "Task unassigned from " + (oldAssignee != null ? oldAssignee : "unassigned"));
                // Optionally notify old assignee
                if (oldAssignee != null) {
                    System.out.println("Notification: Task " + taskId + " has been unassigned from you and returned to the queue.");
                }
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Task unassigned successfully");
                response.put("taskId", taskId);
                response.put("oldAssignee", oldAssignee);
                response.put("status", "UNASSIGNED");
                return ResponseEntity.ok(response);
            }
            // Validate new assignee (implement your own user validation logic)
            if (!taskService.isValidAssignee(newAssignee)) {
                return ResponseEntity.badRequest().body("Invalid target user");
            }
            // Update task assignment
            taskService.assignTask(taskId, newAssignee);
            // Update status
            taskService.updateTaskStatus(taskId, "ASSIGNED");
            // Audit log
            auditService.logTaskAction(taskId, "TASK_REASSIGNED", performedBy,
                "Task reassigned from " + (oldAssignee != null ? oldAssignee : "unassigned") + " to " + newAssignee);
            // Notification (stub)
            if (oldAssignee != null && !oldAssignee.equals(newAssignee)) {
                // Notify old assignee
                System.out.println("Notification: Task " + taskId + " has been reassigned from you to " + newAssignee);
            }
            // Notify new assignee
            System.out.println("Notification: Task " + taskId + " has been assigned to you.");
            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Task assigned/reassigned successfully");
            response.put("taskId", taskId);
            response.put("oldAssignee", oldAssignee);
            response.put("newAssignee", newAssignee);
            response.put("status", "ASSIGNED");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
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