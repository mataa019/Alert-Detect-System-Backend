package com.example.alert_detect_system.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.alert_detect_system.Model.TaskModel;
import com.example.alert_detect_system.repo.TaskRepository;

@Service
public class TaskService {
    @Autowired
    private org.flowable.engine.TaskService taskService;
    
    @Autowired
    private TaskRepository taskRepository;
      // Get my tasks from Flowable
    public List<Task> getMyTasks(String assignee) {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }
      // Get group tasks from Flowable
    public List<Task> getGroupTasks(String groupId) {
        return taskService.createTaskQuery()
                .taskCandidateGroup(groupId)
                .list();
    }
      // Assign task to user
    public void assignTask(String taskId, String assignee) {
        taskService.setAssignee(taskId, assignee);
    }
      // Complete task
    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, variables);
    }
      // Get task by ID from Flowable
    public Task getTaskById(String taskId) {
        return taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
    }
    
    // ===== TaskModel Database Operations =====
    
    // Get tasks by case ID from database
    public List<TaskModel> getTasksByCaseId(UUID caseId) {
        return taskRepository.findByCaseId(caseId);
    }
    
    // Get my tasks from database
    public List<TaskModel> getMyTasksFromDB(String assignee) {
        return taskRepository.findByAssignee(assignee);
    }
    
    // Get group tasks from database
    public List<TaskModel> getGroupTasksFromDB(String candidateGroup) {
        return taskRepository.findByCandidateGroup(candidateGroup);
    }
    
    // Save task to database
    public TaskModel saveTask(TaskModel task) {
        return taskRepository.save(task);
    }
    
    // Create task record in database when workflow task is created
    public TaskModel createTaskRecord(UUID caseId, String taskName, String candidateGroup, String processInstanceId) {
        TaskModel task = new TaskModel(caseId, taskName, candidateGroup);
        task.setProcessInstanceId(processInstanceId);
        return taskRepository.save(task);
    }
}