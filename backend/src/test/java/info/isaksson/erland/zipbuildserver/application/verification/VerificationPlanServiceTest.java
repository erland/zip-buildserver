package info.isaksson.erland.zipbuildserver.application.verification;

import info.isaksson.erland.zipbuildserver.domain.model.project.DetectedProject;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectTechnology;
import info.isaksson.erland.zipbuildserver.domain.model.verification.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationPlanSelection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationPlanServiceTest {
    @Test
    void loadsDefaultPlansFromResources() {
        VerificationPlanService service = new VerificationPlanService();

        List<VerificationPlan> plans = service.listPlans();

        assertEquals(3, plans.size());
        assertTrue(plans.stream().anyMatch(plan -> plan.id().equals("node-default")));
        assertTrue(plans.stream().anyMatch(plan -> plan.id().equals("maven-default")));
        assertTrue(plans.stream().anyMatch(plan -> plan.id().equals("multi-project-default")));
    }

    @Test
    void parsesPlanCommands() {
        VerificationPlan plan = VerificationPlanService.parsePlan("""
                id: example-node
                name: Example Node
                technology: NODE
                enabled: true
                networkMode: DEPENDENCY
                selectionReason: Example selection.
                indicators:
                  - package.json
                commands:
                  - label: Install
                    workingDirectory: ${project.path}
                    commandDisplay: npm ci
                    timeoutSeconds: 120
                    optional: false
                """);

        assertEquals("example-node", plan.id());
        assertEquals(ProjectTechnology.NODE, plan.technology());
        assertEquals(NetworkMode.DEPENDENCY, plan.networkMode());
        assertEquals(List.of("package.json"), plan.indicators());
        assertEquals("npm ci", plan.commands().getFirst().commandDisplay());
        assertEquals(120, plan.commands().getFirst().timeoutSeconds());
        assertFalse(plan.commands().getFirst().optional());
    }

    @Test
    void selectsPlanByDetectedProjectTechnology() {
        VerificationPlanService service = new VerificationPlanService();
        DetectedProject project = new DetectedProject(
                ".",
                ProjectTechnology.MAVEN,
                List.of("pom.xml"),
                null,
                "Detected Maven project indicator.");

        VerificationPlanSelection selection = service.selectPlan(project);

        assertTrue(selection.selected());
        assertEquals("maven-default", selection.selectedPlanId());
    }
}
