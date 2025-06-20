package com.example.alert_detect_system.config;

import org.flowable.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FlowableProcessDeployment implements CommandLineRunner {

    @Autowired
    private RepositoryService repositoryService;

    @Override
    public void run(String... args) throws Exception {
        // Check if the process is already deployed
        long count = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("caseInvestigationProcess")
                .count();

        if (count == 0) {
            // Deploy the BPMN process
            repositoryService.createDeployment()
                    .addClasspathResource("case-process.bpmn20.xml")
                    .deploy();
            
            System.out.println("✅ Successfully deployed caseInvestigationProcess BPMN definition");
        } else {
            System.out.println("✅ caseInvestigationProcess already deployed");
        }
    }
}
