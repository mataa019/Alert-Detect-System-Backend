package com.example.alert_detect_system.service;

import org.flowable.engine.TaskService as FlowableTaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TaskService {
    
    @Autowired
    private FlowableTaskService flowableTaskService;
    
    // Get my tasks
    public List<Task> getMyTasks(String assignee) {
        return flowableTaskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }
    
    // Get group tasks
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
    
    // Get task by ID
    public Task getTaskById(String taskId) {
        return flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
    }
}