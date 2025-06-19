package com.example.alert_detect_system.config;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class FlowableConfigTest {

    @Autowired(required = false)
    private ProcessEngine processEngine;

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Autowired(required = false)
    private TaskService taskService;

    @Test
    public void testFlowableBeans() {
        System.out.println("ProcessEngine: " + processEngine);
        System.out.println("RuntimeService: " + runtimeService);
        System.out.println("TaskService: " + taskService);
        
        // These should not be null if Flowable auto-configuration is working
        assertNotNull(processEngine, "ProcessEngine should be available");
        assertNotNull(runtimeService, "RuntimeService should be available");
        assertNotNull(taskService, "TaskService should be available");
    }
}
