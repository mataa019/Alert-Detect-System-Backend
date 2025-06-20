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

import com.example.alert_detect_system.Model.TaskModel;
import com.example.alert_detect_system.dto.TaskAssignDto;
import com.example.alert_detect_system.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
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
}