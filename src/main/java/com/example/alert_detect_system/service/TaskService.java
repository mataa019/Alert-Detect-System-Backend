package com.example.alert_detect_system.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.alert_detect_system.Model.TaskModel;
import com.example.alert_detect_system.repo.TaskRepository;

@Service
public class TaskService {
    
    @Autowired
    @Qualifier("flowableTaskService")
    private org.flowable.engine.TaskService flowableTaskService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    // Get my tasks from Flowable
    public List<Task> getMyTasks(String assignee) {
        return flowableTaskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }
    
    // Get group tasks from Flowable
    public List<Task> getGroupTasks(String groupId) {
        return flowableTaskService.createTaskQuery()
                .taskCandidateGroup(groupId)
                .list();
    }
    
    // Assign task to user
    public void assignTask(String taskId, String assignee) {
        flowableTaskService.setAssignee(taskId, assignee);
    }
    
    // Complete task
    public void completeTask(String taskId, Map<String, Object> variables) {
        flowableTaskService.complete(taskId, variables);
    }
    
    // Get task by ID from Flowable
    public Task getTaskById(String taskId) {
        return flowableTaskService.createTaskQuery()
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