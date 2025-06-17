package com.example.alert_detect_system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.alert_detect_system.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    // Get my assigned tasks
    @GetMapping("/my/{assignee}")
    public ResponseEntity<List<Task>> getMyTasks(@PathVariable String assignee) {
        List<Task> tasks = taskService.getMyTasks(assignee);
        return ResponseEntity.ok(tasks);
    }
    
    // Get group tasks (work queue)
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Task>> getGroupTasks(@PathVariable String groupId) {
        List<Task> tasks = taskService.getGroupTasks(groupId);
        return ResponseEntity.ok(tasks);
    }
    
    // Get specific task
    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(@PathVariable String taskId) {
        Task task = taskService.getTaskById(taskId);
        if (task != null) {
            return ResponseEntity.ok(task);
        }
        return ResponseEntity.notFound().build();
    }
    
    // Assign/claim task
    @PostMapping("/{taskId}/assign")
    public ResponseEntity<String> assignTask(@PathVariable String taskId, 
                                           @RequestParam String assignee) {
        try {
            taskService.assignTask(taskId, assignee);
            return ResponseEntity.ok("Task assigned successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Complete task
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<String> completeTask(@PathVariable String taskId, 
                                             @RequestBody(required = false) Map<String, Object> variables) {
        try {
            if (variables == null) {
                variables = new HashMap<>();
            }
            taskService.completeTask(taskId, variables);
            return ResponseEntity.ok("Task completed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}