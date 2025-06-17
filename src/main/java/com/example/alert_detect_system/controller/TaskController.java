package com.example.alert_detect_system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alert_detect_system.Model.TaskModel;
import com.example.alert_detect_system.dto.TaskAssignDto;
import com.example.alert_detect_system.service.TaskService;

@RestController
@RequestMapping("/tasks")
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
            return ResponseEntity.ok("Task completed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
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
}