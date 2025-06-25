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
    }    /**
     * Create a new task (for User Stories 1 & 2)
     */
    public TaskModel createTask(String title, String description, UUID caseId, String assignee, String priority) {
        TaskModel newTask = new TaskModel();
        newTask.setTitle(title);
        newTask.setTaskName(title); // Also set taskName for compatibility
        newTask.setDescription(description);
        newTask.setCaseId(caseId);
        newTask.setAssignee(assignee);
        newTask.setPriority(priority);
        // status and createdAt are set by constructor
        
        return taskRepository.save(newTask);
    }
    
    /**
     * Complete task by case ID and task type
     */
    public void completeTaskByCaseIdAndType(UUID caseId, String taskTitle, String completedBy) {
        List<TaskModel> tasks = taskRepository.findByCaseId(caseId);
        
        for (TaskModel task : tasks) {
            if (taskTitle.equals(task.getTitle()) && "OPEN".equals(task.getStatus())) {
                task.setStatus("COMPLETED");
                task.setCompletedBy(completedBy);
                task.setCompletedAt(java.time.LocalDateTime.now());
                taskRepository.save(task);
                break;
            }
        }
    }
    
    /**
     * Get tasks by type/title
     */
    public List<TaskModel> getTasksByType(String taskTitle) {
        return taskRepository.findAll().stream()
            .filter(task -> taskTitle.equals(task.getTitle()))
            .toList();
    }
    
    /**
     * Get tasks by assignee (alias for getMyTasksFromDB for consistency with API)
     */
    public List<TaskModel> getTasksByAssignee(String assignee) {
        return getMyTasksFromDB(assignee);
    }
    
    /**
     * Get all tasks (Admin function)
     */
    public List<TaskModel> getAllTasks() {
        return taskRepository.findAll();
    }
    
    /**
     * Create "Investigate Case" task for approved cases
     */
    public void createInvestigateTask(UUID caseId, String groupId) {
        // Create database task
        TaskModel investigateTask = new TaskModel();
        investigateTask.setCaseId(caseId);
        investigateTask.setTaskName("Investigate Case");
        investigateTask.setCandidateGroup(groupId);
        investigateTask.setStatus("ACTIVE");
        investigateTask.setCreatedAt(java.time.LocalDateTime.now());
        investigateTask.setDescription("Investigate the approved case for potential violations");
        
        taskRepository.save(investigateTask);
    }
    
    /**
     * Create "Complete Case Creation" task for rejected cases
     */
    public void createCompleteTaskForUser(UUID caseId, String assignee) {
        // Create database task
        TaskModel completeTask = new TaskModel();
        completeTask.setCaseId(caseId);
        completeTask.setTaskName("Complete Case Creation");
        completeTask.setAssignee(assignee);
        completeTask.setStatus("ACTIVE");
        completeTask.setCreatedAt(java.time.LocalDateTime.now());
        completeTask.setDescription("Complete the case creation with required information");
        
        taskRepository.save(completeTask);
    }
    
    /**
     * Get pending approval tasks (for supervisors)
     */
    public List<TaskModel> getPendingApprovalTasks() {
        return taskRepository.findByTaskNameAndStatus("Approve Case Creation", "ACTIVE");
    }
    
    /**
     * Create "Approve Case Creation" task when case is completed
     */
    public void createApprovalTask(UUID caseId, String originalCreator) {
        // Create database task for admin group to approve
        TaskModel approvalTask = new TaskModel();
        approvalTask.setCaseId(caseId);
        approvalTask.setTaskName("Approve Case Creation");
        approvalTask.setCandidateGroup("admin"); // Admin group
        approvalTask.setStatus("ACTIVE");
        approvalTask.setCreatedAt(java.time.LocalDateTime.now());
        approvalTask.setDescription("Review and approve or reject the case creation. Original creator: " + originalCreator);
        
        taskRepository.save(approvalTask);
    }
    
    /**
     * Get approved approval tasks (for admin approvals view)
     */
    public List<TaskModel> getApprovedApprovalTasks() {
        return taskRepository.findByTaskNameAndStatus("Approve Case Creation", "COMPLETED");
    }
    
    /**
     * Close the draft ("Complete New Case") task for a case when abandoned
     */
    public void closeDraftTaskForCase(UUID caseId) {
        List<TaskModel> tasks = taskRepository.findByCaseId(caseId);
        for (TaskModel task : tasks) {
            if (("Complete Case Creation".equalsIgnoreCase(task.getTaskName()) || "Complete New Case".equalsIgnoreCase(task.getTaskName()))
                && "OPEN".equalsIgnoreCase(task.getStatus())) {
                task.setStatus("COMPLETED");
                task.setCompletedAt(java.time.LocalDateTime.now());
                taskRepository.save(task);
            }
        }
    }
}