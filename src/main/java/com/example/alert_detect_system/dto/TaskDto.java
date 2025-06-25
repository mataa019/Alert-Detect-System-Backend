package com.example.alert_detect_system.dto;

import java.util.Date;

public class TaskDto {
    private String id;
    private String name;
    private String assignee;
    private Date createTime;
    private String processInstanceId;
    private String processDefinitionId;
    private String description;

    public TaskDto() {}

    public TaskDto(String id, String name, String assignee, Date createTime, String processInstanceId, String processDefinitionId, String description) {
        this.id = id;
        this.name = name;
        this.assignee = assignee;
        this.createTime = createTime;
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(String processDefinitionId) { this.processDefinitionId = processDefinitionId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
