package com.example.alert_detect_system.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.alert_detect_system.Model.TaskModel;

@Repository
public interface TaskRepository extends JpaRepository<TaskModel, UUID> {
    
    // Find tasks by assignee
    List<TaskModel> findByAssignee(String assignee);
    
    // Find tasks by candidate group
    List<TaskModel> findByCandidateGroup(String candidateGroup);
    
    // Find tasks by case ID
    List<TaskModel> findByCaseId(UUID caseId);
    
    // Find tasks by status
    List<TaskModel> findByStatus(String status);
    
    // Find active tasks (not completed)
    @Query("SELECT t FROM TaskModel t WHERE t.status != 'COMPLETED'")
    List<TaskModel> findActiveTasks();
    
    // Find tasks by process instance ID
    List<TaskModel> findByProcessInstanceId(String processInstanceId);
    
    // Count tasks by assignee
    long countByAssignee(String assignee);
}