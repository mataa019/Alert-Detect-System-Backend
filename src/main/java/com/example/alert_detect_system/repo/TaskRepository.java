package com.example.alert_detect_system.repo;

import com.example.alert_detect_system.entity.Task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import com.example.alert_detect_system.service.TaskService;

@Repository
public interface TaskRepository extends JpaRepository<TaskService, UUID> {
    
    // Find tasks by assignee
    List<TaskService> findByAssignee(String assignee);
    
    // Find tasks by candidate group
    List<TaskService> findByCandidateGroup(String candidateGroup);
    
    // Find tasks by case ID
    List<TaskService> findByCaseId(UUID caseId);
    
    // Find tasks by status
    List<TaskService> findByStatus(String status);
    
    // Find active tasks (not completed)
    @Query("SELECT t FROM Task t WHERE t.status != 'COMPLETED'")
    List<TaskService> findActiveTasks();
    
    // Find tasks by process instance ID
    List<TaskService> findByProcessInstanceId(String processInstanceId);
    
    // Count tasks by assignee
    long countByAssignee(String assignee);
}