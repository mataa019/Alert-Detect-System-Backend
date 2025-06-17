package com.example.alert_detect_system.workflow;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.alert_detect_system.Model.CaseModel;

@Service
public class CaseWorkflowService {
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    public String startCaseWorkflow(CaseModel caseEntity) {
        // Prepare workflow variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("caseId", caseEntity.getId().toString());
        variables.put("caseNumber", caseEntity.getCaseNumber());
        variables.put("caseType", caseEntity.getCaseType());
        variables.put("priority", caseEntity.getPriority());
        variables.put("createdBy", caseEntity.getCreatedBy());
        variables.put("requiresApproval", requiresApproval(caseEntity));
        
        // Start the BPMN process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "caseInvestigationProcess", // This matches your BPMN file process ID
            caseEntity.getCaseNumber(),  // Business key
            variables
        );
        
        return processInstance.getId();
    }
    
    private boolean requiresApproval(CaseModel caseEntity) {
        // Business logic to determine if caseModel needs approval
        return "HIGH".equals(caseEntity.getPriority()) || 
               "CRITICAL".equals(caseEntity.getPriority()) ||
               (caseEntity.getRiskScore() != null && caseEntity.getRiskScore() > 80);
    }
}